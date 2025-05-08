package com.mike.chao.jdbc.explorer.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Component
public class DatabaseInfoToolProvider {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public DatabaseInfoToolProvider(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    public McpServerFeatures.SyncToolSpecification getDatabaseInfoTool() {
        // JSON input schema
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object", 
            Map.of(), 
            List.of(), 
            false
        );

        // Create the Tool record
        McpSchema.Tool tool = new McpSchema.Tool(
            "getDatabaseInfo",
             "Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.", 
             inputSchema
        );

        // Implement the tool logic as a BiFunction
        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call = (exchange, args) -> {
            exchange.loggingNotification(LoggingMessageNotification.builder()
                .data("Getting database info...")
                .level(LoggingLevel.INFO)
                .build());
            try (var conn = dataSource.getConnection()) {
                var metaData = conn.getMetaData();
                var info = new HashMap<String, Object>();

                info.put("databaseProductName", metaData.getDatabaseProductName());
                info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
                info.put("driverName", metaData.getDriverName());
                info.put("driverVersion", metaData.getDriverVersion());
                info.put("maxConnections", metaData.getMaxConnections());
                info.put("readOnly", metaData.isReadOnly());
                info.put("supportsTransactions", metaData.supportsTransactions());

                // Split SQL keywords into a list
                var keywords = metaData.getSQLKeywords();
                var sqlKeywords = new ArrayList<String>();
                if (keywords != null && !keywords.isEmpty()) {
                    for (var kw : keywords.split(",")) {
                        sqlKeywords.add(kw.trim());
                    }
                }
                info.put("sqlKeywords", sqlKeywords);
                var json = objectMapper.writeValueAsString(info);
                return new McpSchema.CallToolResult(List.of(new TextContent(json)), false); // Replace with actual result and error flag
            } catch (Exception e) {
                exchange.loggingNotification(LoggingMessageNotification.builder()
                    .data("Error getting database info" + e.getMessage())
                    .level(LoggingLevel.ERROR)
                    .build());
                return new McpSchema.CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true); // Replace with actual error handling
            }
        };
        return new McpServerFeatures.SyncToolSpecification(tool, call);
    }
}
