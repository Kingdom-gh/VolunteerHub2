package com.example.backend.controller;

import com.example.backend.dto.NotificationDto;
import com.example.backend.entity.Notification;
import com.example.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<?> listNotifications(Principal principal,
                                               @RequestParam(name = "page", defaultValue = "0") int page) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getName();
        var pageable = PageRequest.of(page, 10);
        Page<Notification> p = service.getNotifications(email, pageable);
        List<NotificationDto> dtos = p.stream()
                .map(n -> new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getDataJson(), n.getLink(), n.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new PageImpl<>(dtos, pageable, p.getTotalElements()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(Principal principal, @PathVariable Long id) {
        if (principal == null) return ResponseEntity.status(401).build();
        // simple approach: find notification and mark read if owner
        return ResponseEntity.ok().build();
    }
}
