package com.eap.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import java.util.concurrent.TimeUnit;

/**
 * 基本的 Ollama API 設定，交由 Spring AI 自動建立 OllamaChatModel。
 */
@Configuration
@Slf4j
public class OllamaConfig {

    @Bean
    @ConditionalOnMissingBean
    public OllamaApi ollamaApi(
        @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
        @Value("${spring.ai.ollama.connect-timeout:PT30S}") Duration connectTimeout,
        @Value("${spring.ai.ollama.read-timeout:PT2M}") Duration readTimeout
    ) {
        log.info("配置 Ollama API，服務 URL: {}，connectTimeout: {}，readTimeout: {}", baseUrl, connectTimeout, readTimeout);

        // Blocking client設定 (RestClient)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout.toMillis());
        requestFactory.setReadTimeout((int) readTimeout.toMillis());

        RestClient.Builder restClientBuilder = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory);

        // Reactive client設定 (WebClient)
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
            .responseTimeout(readTimeout)
            // 打開 wiretap 以便觀察 HTTP chunk / TCP 細節
            .wiretap(true)
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS)));

        WebClient.Builder webClientBuilder = WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient));

        return new OllamaApi(baseUrl, restClientBuilder, webClientBuilder);
    }
}
