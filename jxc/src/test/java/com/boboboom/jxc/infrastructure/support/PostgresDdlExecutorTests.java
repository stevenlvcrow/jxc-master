package com.boboboom.jxc.infrastructure.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PostgresDdlExecutorTests {

    @Autowired
    private PostgresDdlExecutor postgresDdlExecutor;

    @Test
    void shouldInspectTableExists() {
        assertThat(postgresDdlExecutor.currentSchema()).isNotBlank();
        assertThat(postgresDdlExecutor.tableExists("SYS_USER")).isTrue();
    }
}

