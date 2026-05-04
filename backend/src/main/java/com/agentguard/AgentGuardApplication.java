package com.agentguard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.agentguard.mapper")
public class AgentGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentGuardApplication.class, args);
    }
}
