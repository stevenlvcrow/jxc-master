package com.boboboom.jxc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 后端应用启动入口。 */
@SpringBootApplication
@MapperScan({
    "com.boboboom.jxc.infrastructure.persistence.mapper",
    "com.boboboom.jxc.cost.infrastructure.persistence.mapper",
    "com.boboboom.jxc.finance.infrastructure.persistence.mapper",
    "com.boboboom.jxc.identity.infrastructure.persistence.mapper",
    "com.boboboom.jxc.item.infrastructure.persistence.mapper",
    "com.boboboom.jxc.inventory.infrastructure.persistence.mapper",
    "com.boboboom.jxc.purchase.infrastructure.persistence.mapper",
    "com.boboboom.jxc.workflow.infrastructure.persistence.mapper",
    "com.boboboom.jxc.system.infrastructure.persistence.mapper"
})
public class JxcApplication {

    /** 启动进销存后端应用。 */
    public static void main(String[] args) {
        SpringApplication.run(JxcApplication.class, args);
    }
}
