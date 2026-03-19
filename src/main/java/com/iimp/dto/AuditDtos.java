package com.iimp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

public class AuditDtos {

    @Data @Builder
    public static class AuditResponse {
        private Long id;
        private String action;
        private String oldValue;
        private String newValue;
        private String changedByName;
        private LocalDateTime createdAt;
    }
}
