package com.yuki.test.config;

import com.yuki.test.interceptor.AuthInterceptor;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
@EnableConfigurationProperties({YukiAiProperties.class, YukiJwtProperties.class, YukiCryptoProperties.class})
public class YukiWebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public YukiWebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/yuki/**")
                .excludePathPatterns(
                        "/api/yuki/user/login",
                        "/api/yuki/user/register",
                        "/api/yuki/papers/**",
                        "/api/yuki/time/**"
                );
    }

    @Bean
    public WebClient.Builder webClientBuilder(YukiAiProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(properties.getStreamTimeoutSeconds()));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    public Executor yukiAiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("yuki-ai-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.initialize();
        return executor;
    }
}
