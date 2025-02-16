package cn.wolfcode.cache;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Configuration
public class AppConfig {
    @Bean
    public ScheduledExecutorService scheduledExecutorService(){
        return new ScheduledThreadPoolExecutor(10);
    }
}
