package com.zzy.project.config;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import io.swagger.models.auth.In;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 重试机制
 */
@Configuration
public class GuavaRetryingConfig {
    @Bean
    public Retryer<Boolean> retryer(){
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.equalTo(false)) // 如果结果为 false 则重试
                .retryIfException() // 如果抛出异常则重试
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.MICROSECONDS)) // 每次重试等待 1 秒
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)) // 尝试 3 次后停止重试
                .build();
        return retryer;
    }

}
