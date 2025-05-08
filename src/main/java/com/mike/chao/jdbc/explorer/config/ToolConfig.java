package com.mike.chao.jdbc.explorer.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mike.chao.jdbc.explorer.ExplorerService;
import com.mike.chao.jdbc.explorer.tools.DatabaseInfoToolProvider;

import io.modelcontextprotocol.server.McpServerFeatures;

@Configuration
public class ToolConfig {

	// higher level API, but less flexible, depends on @Tool annotation in the service class
    // @Bean
	// public List<ToolCallback> explorerTools(ExplorerService explorerService) {
	// 	return List.of(ToolCallbacks.from(explorerService));
	// }

	// lower level API, but more flexible
	@Bean
	public List<McpServerFeatures.SyncToolSpecification> tools(ExplorerService explorerService,  DatabaseInfoToolProvider databaseInfoToolProvider) {
		List<ToolCallback> toolCallBacks = List.of(ToolCallbacks.from(explorerService));
		List<McpServerFeatures.SyncToolSpecification> syncToolSpecs = new ArrayList<>(McpToolUtils.toSyncToolSpecification(toolCallBacks));
		syncToolSpecs.add(databaseInfoToolProvider.getDatabaseInfoTool());
		return syncToolSpecs;
	}
}
