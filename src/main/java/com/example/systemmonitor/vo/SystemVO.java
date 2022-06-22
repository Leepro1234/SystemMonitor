package com.example.systemmonitor.vo;

import com.example.systemmonitor.common.Util;
import lombok.Data;

@Data
public class SystemVO {
    //파일명
    private String fileName;

    //파일경로
    private String path;

    //shell 생성경로
    private String makePath;

    //shell 파일명
    private String makeFileName;

    //slack Url
    private String webhookUrl;

    //재시도 시간
    private String limitTime;

    //시스템 명
    private String systemName;

    //크론탭 시간
    private String crontabTime;

    //프로세스명
    private String processName;
}
