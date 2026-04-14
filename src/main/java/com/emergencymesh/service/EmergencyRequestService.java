package com.emergencymesh.service;

import com.emergencymesh.dto.EmergencyRequestDto;
import com.emergencymesh.exception.ResourceNotFoundException;
import com.emergencymesh.model.EmergencyRequest;
import com.emergencymesh.model.User;
import com.emergencymesh.repository.EmergencyRequestRepository;
import com.emergencymesh.service.RawEmergencyRequestService;
import com.emergencymesh.model.RawEmergencyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyRequestService {

    private final EmergencyRequestRepository repository;
    private final UserService userService;
    private final LocationService locationService;
    private final ResponderService responderService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final RawEmergencyRequestService rawService;

    @Transactional
    public EmergencyRequestDto.Response createRequest(EmergencyRequestDto.Create requestDto) {
        
        // 1. Save raw exactly as it came in (Robustly, won't rollback)
        rawService.saveRawRequest(requestDto);

        // 2. Normal flow - kullanıcı yoksa fallback kullan (veri kaybını önle)
        User user;
        try {
            user = userService.getUserEntityById(requestDto.getUserId());
        } catch (ResourceNotFoundException e) {
            user = userService.getFallbackUser();
        }

        // Update user's last known location
        com.emergencymesh.dto.LocationDto.Request locRequest = new com.emergencymesh.dto.LocationDto.Request();
        locRequest.setUserId(requestDto.getUserId());
        locRequest.setLatitude(requestDto.getLatitude());
        locRequest.setLongitude(requestDto.getLongitude());
        locationService.updateLocation(locRequest);

        EmergencyRequest request = new EmergencyRequest();
        request.setUser(user);
        request.setType(requestDto.getType());
        
        // Use default description if empty
        String desc = requestDto.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            desc = "Acil durum çağrısı (Açıklama belirtilmedi)";
        }
        request.setDescription(desc);
        
        request.setStatus(EmergencyRequest.RequestStatus.PENDING);
        request.setLatitude(requestDto.getLatitude());
        request.setLongitude(requestDto.getLongitude());
        if (requestDto.getPriority() != null) {
            request.setPriority(EmergencyRequest.EmergencyPriority.valueOf(requestDto.getPriority().toUpperCase()));
        }

        EmergencyRequest saved = repository.save(request);
        
        // ★ KRİTİK: Transaction COMMIT olduktan SONRA gönder (hayalet vaka önlemi)
        EmergencyRequestDto.Response response = EmergencyRequestDto.Response.fromEntity(saved);
        broadcastAfterCommit(response);
        
        return response;
    }

    @Transactional(readOnly = true)
    public Page<EmergencyRequestDto.Response> getAllRequests(Pageable pageable) {
        return repository.findAllWithUser(org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by("createdAt").descending()))
                .map(EmergencyRequestDto.Response::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<EmergencyRequestDto.Response> getRequestsByStatus(EmergencyRequest.RequestStatus status, Pageable pageable) {
        return repository.findByStatusWithUser(status, org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        org.springframework.data.domain.Sort.by("createdAt").descending()))
                .map(EmergencyRequestDto.Response::fromEntity);
    }

    @Transactional
    public EmergencyRequestDto.Response updateStatus(UUID id, EmergencyRequest.RequestStatus newStatus) {
        EmergencyRequest request = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyRequest", "id", id));

        request.setStatus(newStatus);
        if (newStatus == EmergencyRequest.RequestStatus.RESOLVED) {
            request.setPriority(EmergencyRequest.EmergencyPriority.RESOLVED);
            // Also free up the responder if assigned
            if (request.getAssignedResponder() != null) {
                responderService.updateStatus(request.getAssignedResponder().getId(), com.emergencymesh.model.Responder.ResponderStatus.AVAILABLE);
            }
        }
        EmergencyRequest saved = repository.save(request);
        return EmergencyRequestDto.Response.fromEntity(saved);
    }

    @Transactional
    public EmergencyRequestDto.Response assignResponder(UUID requestId, UUID responderId) {
        EmergencyRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyRequest", "id", requestId));
        
        com.emergencymesh.model.Responder responder = responderService.getResponderEntityById(responderId);
        
        request.setAssignedResponder(responder);
        request.setStatus(EmergencyRequest.RequestStatus.IN_PROGRESS);
        
        // Mark responder as BUSY
        responderService.updateStatus(responderId, com.emergencymesh.model.Responder.ResponderStatus.BUSY);
        
        EmergencyRequest saved = repository.save(request);
        EmergencyRequestDto.Response response = EmergencyRequestDto.Response.fromEntity(saved);
        
        // ★ Broadcast AFTER commit
        broadcastAfterCommit(response);
        
        return response;
    }

    private void broadcastAfterCommit(EmergencyRequestDto.Response response) {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagingTemplate.convertAndSend("/topic/emergencies", response);
                    }
                });
        } else {
            messagingTemplate.convertAndSend("/topic/emergencies", response);
        }
    }
}
