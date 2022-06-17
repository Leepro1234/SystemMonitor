package com.example.systemmonitor.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackVO {
    private String text;

    private String system;

    private String logFileName;

    private String keyword;

    private String webhookUrl;

    private String limitTime;
}
