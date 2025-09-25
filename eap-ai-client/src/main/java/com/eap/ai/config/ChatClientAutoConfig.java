package com.eap.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ChatClientAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(ObjectProvider<ChatModel> chatModelProvider) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model != null) {
            log.info("Creating ChatClient from available ChatModel: {}", model.getClass().getName());
            return ChatClient.create(model);
        }
        log.warn("No ChatModel available to create ChatClient. ChatClient bean will not be created.");
        return null;
    }
}
