package com.mike.chao.jdbc.explorer.tools;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.stereotype.Component;

import com.mike.chao.jdbc.explorer.resources.BusinessInsights;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;

@Component
public class BusinessInsightsToolProvider {

    private final BusinessInsights businessInsights;

    public BusinessInsightsToolProvider(BusinessInsights businessInsights) {
        this.businessInsights = businessInsights;
    }

    public McpServerFeatures.SyncToolSpecification getAddBusinessInsightsTool() {
        // JSON input schema
        String inputSchema = """
        {
            "type": "object",
            "properties": {
                "insights": {
                    "type": "string",
                    "description": "business insight discovered during data analysis"
                }
            },
            "required": ["insights"],
            "additionalProperties": false
        }
                """;

        // Create the Tool record
        McpSchema.Tool tool = new McpSchema.Tool(
            "addBusinessInsight",
             "Add a business insight discovered during data analysis to the memo.", 
             inputSchema
        );

        // Implement the tool logic as a BiFunction
        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call = (exchange, args) -> {
            Object insight = args.get("insights");
            exchange.loggingNotification(LoggingMessageNotification.builder()
                .data("Adding business insights...")
                .level(LoggingLevel.INFO)
                .build());
            if (insight != null && insight instanceof String) {
                businessInsights.addInsight(insight.toString());
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("Business insight added successfully.")), false);
            } else {
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent("Invalid input. Please provide a valid business insight.")), true);
            }
        };

        return new McpServerFeatures.SyncToolSpecification(tool, call);
    }
}
