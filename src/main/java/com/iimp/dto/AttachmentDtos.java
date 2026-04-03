package com.iimp.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

public class AttachmentDtos {

    @Data @Builder
    public static class AttachmentResponse {
        private Long id;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String contentType;
        private String uploadedByName;
        private LocalDateTime createdAt;
        
       
    }
}
