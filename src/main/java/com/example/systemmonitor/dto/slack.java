package com.example.systemmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class slack {
    private String text;

    private String system;

    private String fileName;

    private String keyword;
}
