package com.example.systemmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SystemMonitorApplication  extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(SystemMonitorApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder bulder){
        return bulder.sources(SystemMonitorApplication.class);
    }

}
