package com.iimp.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketVolumeDto {
    private String label;
    private long count;
}
