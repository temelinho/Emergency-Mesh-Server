package com.emergencymesh.service;

import com.emergencymesh.dto.ResponderDto;
import com.emergencymesh.exception.ResourceNotFoundException;
import com.emergencymesh.model.Responder;
import com.emergencymesh.repository.ResponderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponderService {

    private final ResponderRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ResponderDto.Response createResponder(ResponderDto.Request request) {
        Responder responder = new Responder();
        responder.setName(request.getName());
        responder.setType(request.getType());
        responder.setStatus(Responder.ResponderStatus.AVAILABLE);
        responder.setLatitude(request.getLatitude());
        responder.setLongitude(request.getLongitude());
        responder.setCity(request.getCity());
        responder.setDistrict(request.getDistrict());
        
        Responder saved = repository.save(responder);
        broadcastUpdate();
        return ResponderDto.Response.fromEntity(saved);
    }

    @Transactional
    public ResponderDto.Response updateLocation(UUID id, Double lat, Double lon) {
        Responder responder = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responder", "id", id));
        
        responder.setLatitude(lat);
        responder.setLongitude(lon);
        Responder saved = repository.save(responder);
        broadcastUpdate();
        return ResponderDto.Response.fromEntity(saved);
    }

    @Transactional
    public ResponderDto.Response updateStatus(UUID id, Responder.ResponderStatus status) {
        Responder responder = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responder", "id", id));
        
        responder.setStatus(status);
        Responder saved = repository.save(responder);
        broadcastUpdate();
        return ResponderDto.Response.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ResponderDto.Response> getAllResponders() {
        return repository.findAll().stream()
                .map(ResponderDto.Response::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Responder getResponderEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Responder", "id", id));
    }

    private void broadcastUpdate() {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagingTemplate.convertAndSend("/topic/responders", getAllResponders());
                    }
                });
        } else {
            messagingTemplate.convertAndSend("/topic/responders", getAllResponders());
        }
    }
}
