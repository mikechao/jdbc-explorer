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

/**
 * Provides a tool to add business insights discovered during data analysis.
 * This tool integrates with the Model Context Protocol (MCP) framework.
 */
@Component
public class BusinessInsightsToolProvider {

    private final BusinessInsights businessInsights;

    private static final String TOOL_NAME = "addBusinessInsight";
    private static final String TOOL_DESCRIPTION = "Add a business insight discovered during data analysis to the memo.";
    private static final String INSIGHTS_ARG_KEY = "insights";

    private static final McpSchema.JsonSchema INPUT_SCHEMA = new McpSchema.JsonSchema(
        "object",
        Map.of(
            INSIGHTS_ARG_KEY, Map.of( // Use constant for key
                "type", "string",
                "description", "business insight discovered during data analysis"
            )
        ),
        List.of(INSIGHTS_ARG_KEY), // Use constant for key
        false // additionalProperties
    );

    /**
     * Constructs a {@link BusinessInsightsToolProvider} with the necessary dependencies.
     *
     * @param businessInsights The service responsible for storing business insights.
     */
    public BusinessInsightsToolProvider(BusinessInsights businessInsights) {
        this.businessInsights = businessInsights;
    }

    /**
     * Gets the MCP tool specification for adding business insights.
     *
     * @return A {@link McpServerFeatures.SyncToolSpecification} that defines the tool's
     *         name, description, input schema, and execution logic.
     */
    public McpServerFeatures.SyncToolSpecification getAddBusinessInsightsTool() {
        // Create the Tool record
        var tool = new McpSchema.Tool(
            TOOL_NAME,
            TOOL_DESCRIPTION,             
            INPUT_SCHEMA
        );

        // Implement the tool logic as a BiFunction
        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> call = (exchange, args) -> {
            Object insight = args.get("insights");
            exchange.loggingNotification(LoggingMessageNotification.builder()
                .data("Adding business insights...")
                .level(LoggingLevel.INFO)
                .build());
            var result = switch (insight) {
                case String insightText when !insightText.isBlank() -> {
                    businessInsights.addInsight(insightText);
                    logToolActivity(exchange, "Business insight added successfully.", LoggingLevel.INFO);
                    yield new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent("Business insight added successfully.")), 
                        false
                    );
                }
                case null -> {
                    logToolActivity(exchange, "Error: Business insight input was null.", LoggingLevel.ERROR);
                    yield new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(
                        """
                        {"error": "NullInput", "message": "Business insight cannot be null"}
                        """
                    )), 
                    true
                    );
                }
                default -> {
                    String errorMessage = String.format(
                        """
                        {"error": "InvalidInputType", "message": "Invalid input type for insight. Expected String.", "receivedType": "%s"}
                        """,
                        insight.getClass().getSimpleName()
                    );
                    logToolActivity(exchange, "Error: " + errorMessage, LoggingLevel.ERROR);
                    yield new McpSchema.CallToolResult(
                        List.of(new McpSchema.TextContent(errorMessage)), 
                        true
                    );
                } 
                

            };
            return result;
        };

        return new McpServerFeatures.SyncToolSpecification(tool, call);
    }

    private void logToolActivity(McpSyncServerExchange exchange, String message, LoggingLevel level) {
        exchange.loggingNotification(LoggingMessageNotification.builder()
            .data(message)
            .level(level)
            .build());
    }
}
