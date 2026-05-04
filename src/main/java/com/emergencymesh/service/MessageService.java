package com.emergencymesh.service;

import com.emergencymesh.dto.MessageDto;
import com.emergencymesh.model.Message;
import com.emergencymesh.model.User;
import com.emergencymesh.repository.MessageRepository;
import com.emergencymesh.repository.EmergencyRequestRepository;
import com.emergencymesh.model.RawEmergencyRequest;
import com.emergencymesh.model.EmergencyRequest;
import com.emergencymesh.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RawEmergencyRequestService rawService;
    private final AiTriageClient aiTriageClient;

    @Transactional
    public MessageDto.Response sendMessage(MessageDto.Request request) {
        // 0. Tekilleştirme (Deduplication) Kontrolü
        if (request.getId() != null) {
            if (messageRepository.existsById(request.getId())) {
                // Önceden bu veya başka bir gateway tarafından senkronize edilmiş.
                // İstemciye başarılıymış gibi dönüyoruz ki kendi cihazında "Gönderildi" olarak işaretlesin.
                Message existingMessage = messageRepository.findById(request.getId()).orElseThrow();
                return MessageDto.Response.fromEntity(existingMessage);
            }
        }

        // 1. Önce parse et (normalizer tüm formatları dönüştürür)
        MessageContentNormalizer.ParsedEmergency parsed = MessageContentNormalizer.parse(request.getContent());

        // 2. AI Triage Entegrasyonu (Sadece serbest metinli mesajlar için)
        if (request.getContent() != null && !request.getContent().startsWith("!")) {
            com.emergencymesh.dto.AiTriageResponse aiResponse = aiTriageClient.predictPriority(request.getContent());
            if (aiResponse != null) {
                updateParsedWithAi(parsed, aiResponse);
            }
        }

        boolean isEmergencyRaw = parsed.isEmergency
                || (request.getEmergencyType() != null && !request.getEmergencyType().isBlank());
        if (isEmergencyRaw) {
            rawService.saveRawRequestFromMessage(request);
        }

        // 2. Kullanıcıyı bul; bulunamazsa fallback (mesh_anonim) ile devam et
        User sender;
        UUID senderUuid = null;
        try {
            if (request.getSenderId() != null) {
                senderUuid = UUID.fromString(request.getSenderId());
            }
        } catch (Exception ignored) {}

        if (senderUuid != null) {
            // NOT: UserService yerine doğrudan repository kullanıyoruz ki 'Not Found' hatası transaction'ı zehirlemesin
            sender = userService.getUserRepository().findById(senderUuid).orElse(null);
        } else {
            sender = null;
        }

        if (sender == null) {
            sender = userService.getFallbackUser();
        }

        Message message = new Message();
        if (request.getId() != null) {
            message.setId(request.getId());
        }
        if (request.getOriginalCreatedAt() != null) {
            message.setTimestamp(java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(request.getOriginalCreatedAt()), 
                java.time.ZoneId.of("UTC")));
        }
        message.setSender(sender);

        // 3. Opsiyonel emergencyType alanı (JSON ile gelen, parse'ı geçersiz kılabilir)
        if (request.getEmergencyType() != null && !request.getEmergencyType().isBlank()) {
            try {
                parsed.type = EmergencyRequest.EmergencyType.valueOf(request.getEmergencyType().toUpperCase().trim());
                parsed.priority = parsed.priority != null ? parsed.priority : EmergencyRequest.EmergencyPriority.HIGH;
                parsed.isEmergency = true;
            } catch (IllegalArgumentException ignored) { /* geçersiz tip, parsed değerleri kullanılacak */ }
        }

        String displayContent = parsed.cleanContent != null && !parsed.cleanContent.isBlank()
                ? parsed.cleanContent : (request.getContent() != null ? request.getContent() : "");
        message.setContent(displayContent);
        message.setLatitude(request.getLatitude());
        message.setLongitude(request.getLongitude());
        
        // Priority: request > normalizer (acil durumda) > varsayılan
        if (request.getPriority() != null) {
            try {
                message.setPriority(Message.MessagePriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException e) {
                message.setPriority(Message.MessagePriority.MEDIUM);
            }
        } else if (parsed.isEmergency && parsed.priority != null) {
            message.setPriority(mapEmergencyPriorityToMessage(parsed.priority));
        } else {
            message.setPriority(Message.MessagePriority.LOW);
        }
        
        message.setDelivered(false);

        Message savedMessage = messageRepository.save(message);

        // Eğer normalizer acil durum tespit ettiyse otomatik EmergencyRequest oluştur
        com.emergencymesh.dto.EmergencyRequestDto.Response erResponseFinal = null;
        if (parsed.isEmergency && parsed.type != null) {
            EmergencyRequest emergencyReq = new EmergencyRequest();
            if (request.getOriginalCreatedAt() != null) {
                emergencyReq.setCreatedAt(java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(request.getOriginalCreatedAt()), 
                    java.time.ZoneId.of("UTC")));
            }
            emergencyReq.setUser(sender);
            emergencyReq.setType(parsed.type);
            emergencyReq.setDescription(displayContent);
            emergencyReq.setStatus(EmergencyRequest.RequestStatus.PENDING);
            emergencyReq.setPriority(parsed.priority != null ? parsed.priority : EmergencyRequest.EmergencyPriority.HIGH);
            if (request.getLatitude() != null && request.getLongitude() != null) {
                emergencyReq.setLatitude(request.getLatitude());
                emergencyReq.setLongitude(request.getLongitude());
            }
            EmergencyRequest savedReq = emergencyRequestRepository.save(emergencyReq);
            erResponseFinal = com.emergencymesh.dto.EmergencyRequestDto.Response.fromEntity(savedReq);
        }

        // Notify admins via WebSocket topic about the message
        MessageDto.Response response = MessageDto.Response.fromEntity(savedMessage);

        // ★ KRİTİK: WebSocket bildirimlerini transaction commit sonrası gönder.
        // Eğer transaction synchronization aktif değilse (batch/WS context), direkt gönder.
        final com.emergencymesh.dto.EmergencyRequestDto.Response emergencyToSend = erResponseFinal;
        final MessageDto.Response messageToSend = response;

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        broadcastMessages(emergencyToSend, messageToSend);
                    }
                });
        } else {
            // Fallback: transaction sync aktif değil, direkt gönder
            broadcastMessages(emergencyToSend, messageToSend);
        }

        return response;
    }

    private void broadcastMessages(
            com.emergencymesh.dto.EmergencyRequestDto.Response emergencyToSend,
            MessageDto.Response messageToSend) {
        if (emergencyToSend != null) {
            messagingTemplate.convertAndSend("/topic/emergencies", emergencyToSend);
        }
        if (messageToSend != null) {
            messagingTemplate.convertAndSend("/topic/messages", messageToSend);
        }
    }

    private static Message.MessagePriority mapEmergencyPriorityToMessage(EmergencyRequest.EmergencyPriority p) {
        if (p == null) return Message.MessagePriority.LOW;
        return switch (p) {
            case EMERGENCY -> Message.MessagePriority.EMERGENCY;
            case HIGH -> Message.MessagePriority.HIGH;
            case MEDIUM -> Message.MessagePriority.MEDIUM;
            default -> Message.MessagePriority.LOW;
        };
    }

    @Transactional(readOnly = true)
    public Page<MessageDto.Response> getMessagesForUser(UUID userId, Pageable pageable) {
        return messageRepository.findByUserId(userId, pageable)
                .map(MessageDto.Response::fromEntity);
    }

    // Batch işleminde bir mesajın hatası diğerlerini bozmasın diye @Transactional'ı kaldırdık.
    // Her bir sendMessage çağrısı kendi içinde @Transactional olduğu için bağımsız commit edilir.
    public void sendMessages(java.util.List<MessageDto.Request> requests) {
        if (requests == null) return;
        System.out.println("[BATCH] Received batch of " + requests.size() + " messages");
        for (int i = 0; i < requests.size(); i++) {
            MessageDto.Request request = requests.get(i);
            try {
                sendMessage(request);
                System.out.println("[BATCH] Successfully processed message " + (i + 1) + "/" + requests.size() + (request.getContent() != null ? " (Content: " + request.getContent() + ")" : ""));
            } catch (Exception e) {
                System.err.println("[BATCH] Error processing message " + (i + 1) + ": " + e.getMessage());
            }
        }
    }
    private void updateParsedWithAi(MessageContentNormalizer.ParsedEmergency parsed, com.emergencymesh.dto.AiTriageResponse ai) {
        if (ai == null || ai.getPriority() == null) return;
        
        try {
            EmergencyRequest.EmergencyPriority aiPri = EmergencyRequest.EmergencyPriority.valueOf(ai.getPriority().toUpperCase());
            
            // AI 'EMERGENCY' veya 'HIGH' diyorsa ve kurallar daha düşük diyorsa AI'yı dinle
            if (aiPri.ordinal() >= EmergencyRequest.EmergencyPriority.HIGH.ordinal()) {
                parsed.priority = aiPri;
                parsed.isEmergency = true;
                // Eğer normalizer bir tür belirlemediyse, AI genel acil durum olarak işaretler
                if (parsed.type == null) {
                    parsed.type = EmergencyRequest.EmergencyType.OTHER;
                }
            } else if (parsed.priority == null) {
                parsed.priority = aiPri;
                // MEDIUM ve LOW seviye AI tahminlerinde de tür yoksa ata
                if (parsed.type == null && aiPri.ordinal() >= EmergencyRequest.EmergencyPriority.MEDIUM.ordinal()) {
                    parsed.type = EmergencyRequest.EmergencyType.OTHER;
                    parsed.isEmergency = true;
                }
            }
        } catch (Exception ignored) {}
    }
}
