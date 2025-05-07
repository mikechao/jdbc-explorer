package com.mike.chao.jdbc.explorer.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mike.chao.jdbc.explorer.prompts.ExplorerPromptProvider;

import io.modelcontextprotocol.server.McpServerFeatures;


@Configuration
public class PromptConfig {

    private final ExplorerPromptProvider explorerPromptProvider;

    public PromptConfig(ExplorerPromptProvider explorerPromptProvider) {
        this.explorerPromptProvider = explorerPromptProvider;
    }
    
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> prompts() {
        return List.of(this.explorerPromptProvider.createPrompt());
    }
}
