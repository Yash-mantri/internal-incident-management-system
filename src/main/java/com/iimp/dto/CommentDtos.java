package com.iimp.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Data@Builder
    public static class CommentDTO {
    	  private Long  id;
          private String commentText;
          private boolean isInternal;
          private LocalDateTime createdAt;
          private CommentUserDTO user;
    }
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommentUserDTO {
        private String name;
    }
}
