package com.emergencymesh.service;

import com.emergencymesh.dto.EmergencyRequestDto;
import com.emergencymesh.model.RawEmergencyRequest;
import com.emergencymesh.repository.RawEmergencyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RawEmergencyRequestService {

    private final RawEmergencyRequestRepository rawRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRawRequest(EmergencyRequestDto.Create requestDto) {
        RawEmergencyRequest raw = new RawEmergencyRequest();
        raw.setUserId(requestDto.getUserId());
        raw.setType(requestDto.getType() != null ? requestDto.getType().name() : null);
        raw.setDescription(requestDto.getDescription());
        raw.setLatitude(requestDto.getLatitude());
        raw.setLongitude(requestDto.getLongitude());
        raw.setPriority(requestDto.getPriority());
        rawRepository.save(raw);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRawRequestFromTag(java.util.UUID userId, String type, String content, Double lat, Double lng) {
        RawEmergencyRequest raw = new RawEmergencyRequest();
        raw.setUserId(userId);
        raw.setType(type);
        raw.setDescription("TAG: " + content);
        raw.setLatitude(lat);
        raw.setLongitude(lng);
        raw.setPriority("HIGH");
        rawRepository.save(raw);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRawRequestFromMessage(com.emergencymesh.dto.MessageDto.Request request) {
        RawEmergencyRequest raw = new RawEmergencyRequest();
        raw.setUserId(request.getSenderId());
        raw.setType(request.getEmergencyType() != null ? request.getEmergencyType() : "TAG_CHECK");
        raw.setDescription("MSG: " + (request.getContent() != null ? request.getContent() : ""));
        raw.setLatitude(request.getLatitude());
        raw.setLongitude(request.getLongitude());
        raw.setPriority(request.getPriority() != null ? request.getPriority() : "HIGH");
        rawRepository.save(raw);
    }
}
