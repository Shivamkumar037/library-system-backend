package com.library.service;

import com.library.exception.GlobalExceptionHandler;
import com.library.model.AdminActionLog;
import com.library.model.User;
import com.library.repository.AdminActionRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// ================= USER & ADMIN SERVICE =================
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final AdminActionRepository actionRepository;

    public void deleteUser(Long targetUserId, Long adminId) {
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("User not found"));
        target.setActive(false);
        userRepository.save(target);

        actionRepository.save(AdminActionLog.builder()
                .adminId(adminId)
                .targetUserId(targetUserId)
                .action("DELETE_USER")
                .timestamp(LocalDateTime.now()).build());
    }
}
