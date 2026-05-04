package com.emergencymesh.service;

import com.emergencymesh.dto.UserDto;
import com.emergencymesh.exception.ResourceNotFoundException;
import com.emergencymesh.model.User;
import com.emergencymesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto.Response registerUser(UserDto.Request request) {
        // Kullanıcı adı benzersiz olmalı — başka bir telefon numarası kullanıyorsa reddet
        if (userRepository.existsByUsername(request.getUsername())) {
            // Aynı telefon numarasıyla kayıt güncellemesi ise izin ver
            boolean samePhone = userRepository.findByUsername(request.getUsername())
                    .map(u -> u.getPhoneNumber().equals(request.getPhoneNumber()))
                    .orElse(false);
            if (!samePhone) {
                throw new RuntimeException("Bu kullanıcı adı zaten alınmış. Lütfen başka bir kullanıcı adı seçin.");
            }
        }

        // Check if user already exists with same phone number
        User existingUser = userRepository.findByPhoneNumber(request.getPhoneNumber()).orElse(null);

        if (existingUser != null) {
            // Update fields if changed
            existingUser.setUsername(request.getUsername());
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            existingUser.setCity(request.getCity());
            existingUser.setDistrict(request.getDistrict());
            User saved = userRepository.save(existingUser);
            return UserDto.Response.fromEntity(saved);
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPhoneNumber(request.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setCity(request.getCity());
        newUser.setDistrict(request.getDistrict());
        User saved = userRepository.save(newUser);
        return UserDto.Response.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public UserDto.Response login(UserDto.LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("Geçersiz telefon numarası veya şifre"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Geçersiz telefon numarası veya şifre");
        }

        return UserDto.Response.fromEntity(user);
    }

    @Transactional
    public UserDto.Response updateUser(UUID id, UserDto.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Check if phone number is changing and already taken by another user
        if (!user.getPhoneNumber().equals(request.getPhoneNumber())) {
            userRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("Bu telefon numarası zaten kullanımda.");
                }
            });
        }

        // Check if username is changing and already taken by another user
        if (!user.getUsername().equals(request.getUsername())) {
            userRepository.findByUsername(request.getUsername()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("Bu kullanıcı adı zaten alınmış. Lütfen başka bir kullanıcı adı seçin.");
                }
            });
        }

        user.setUsername(request.getUsername());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCity(request.getCity());
        user.setDistrict(request.getDistrict());

        User saved = userRepository.save(user);
        return UserDto.Response.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public UserDto.Response getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserDto.Response.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public User getFallbackUser() {
        return userRepository.findByUsername("mesh_anonim")
                .orElseThrow(() -> new RuntimeException("Fallback user mesh_anonim not found in database"));
    }

    @Transactional(readOnly = true)
    public java.util.List<UserDto.Response> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDto.Response::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }
}
