package cn.wolfcode.web.config;

import cn.wolfcode.common.web.interceptor.FeignRequestInterceptor;
import cn.wolfcode.common.web.interceptor.RequireLoginInterceptor;
import cn.wolfcode.common.web.resolver.UserInfoMethodArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;


@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public ThreadPoolTaskExecutor webAsyncThreadPoolExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(20);
        threadPoolTaskExecutor.setKeepAliveSeconds(3);
        return threadPoolTaskExecutor;
    }
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer){
        configurer.setDefaultTimeout(3000);
        configurer.setTaskExecutor(webAsyncThreadPoolExecutor());
    }
    @Bean
    public TimeoutCallableProcessingInterceptor timeoutCallableProcessingInterceptor(){
        return new TimeoutCallableProcessingInterceptor();
    }
    @Bean
    public RequireLoginInterceptor requireLoginInterceptor(StringRedisTemplate redisTemplate){
        return new RequireLoginInterceptor(redisTemplate);
    }
    @Bean
    public FeignRequestInterceptor feignRequestInterceptor(){
        return new FeignRequestInterceptor();
    }
    @Autowired
    private RequireLoginInterceptor requireLoginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireLoginInterceptor)
                .addPathPatterns("/**");
    }
    @Bean
    public UserInfoMethodArgumentResolver userInfoMethodArgumentResolver(){
        return new UserInfoMethodArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userInfoMethodArgumentResolver());
    }
}
