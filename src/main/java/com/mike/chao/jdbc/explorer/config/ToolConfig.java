package com.mike.chao.jdbc.explorer.config;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mike.chao.jdbc.explorer.ExplorerService;

@Configuration
public class ToolConfig {

    @Bean
	public List<ToolCallback> explorerTools(ExplorerService explorerService) {
		return List.of(ToolCallbacks.from(explorerService));
	}
}
