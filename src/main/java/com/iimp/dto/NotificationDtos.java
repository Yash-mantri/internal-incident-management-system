package com.iimp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

public class NotificationDtos {

    @Data @Builder
    public static class NotificationResponse {
        private Long id;
        private String message;
        private boolean read;
        private LocalDateTime createdAt;
    }
}
