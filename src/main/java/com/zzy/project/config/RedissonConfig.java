package com.zzy.project.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() throws IOException {
        // 1. Create config object
        Config config = new Config();
        config.useSingleServer().setDatabase(1).setAddress("redis://127.0.0.1:6379");

// 2. Create Redisson instance

// Sync and Async API
        RedissonClient redisson = Redisson.create(config);

        return redisson;
    }
}
