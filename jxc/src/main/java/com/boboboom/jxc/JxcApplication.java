package com.boboboom.jxc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.boboboom.jxc.infrastructure.persistence.mapper",
        "com.boboboom.jxc.identity.infrastructure.persistence.mapper",
        "com.boboboom.jxc.item.infrastructure.persistence.mapper",
        "com.boboboom.jxc.inventory.infrastructure.persistence.mapper",
        "com.boboboom.jxc.workflow.infrastructure.persistence.mapper",
        "com.boboboom.jxc.system.infrastructure.persistence.mapper"
})
public class JxcApplication {

    public static void main(String[] args) {
        SpringApplication.run(JxcApplication.class, args);
    }
}

