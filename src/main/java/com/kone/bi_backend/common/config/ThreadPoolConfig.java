package com.kone.bi_backend.common.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Thread" + count);
                count++;
                return thread;
            }
        };
        return new ThreadPoolExecutor(
                2,
                4,
                100,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1000),
                threadFactory);
    }
}
