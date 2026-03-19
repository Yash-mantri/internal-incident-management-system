package com.iimp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

public class CommentDtos {

    @Data
    public static class AddCommentRequest {
        @NotBlank private String commentText;
        private boolean internal = false;
    }

    @Data @Builder
    public static class CommentResponse {
        private Long id;
        private String commentText;
        private String authorName;
        private String authorRole;
        private boolean internal;
        private LocalDateTime createdAt;
    }
}
