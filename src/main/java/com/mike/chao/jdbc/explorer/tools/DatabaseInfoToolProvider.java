package com.mike.chao.jdbc.explorer.tools;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Provides a tool for retrieving database metadata through the Model Context Protocol (MCP).
 * This component exposes database information such as product name, version, driver details,
 * and SQL keywords to assist AI models in generating appropriate SQL queries.
 */
@Component
public class DatabaseInfoToolProvider {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private static final McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object", 
            Map.of(), 
            List.of(), 
            false
    );

    public record DatabaseInfo(
        String databaseProductName,
        String databaseProductVersion,
        String driverName, 
        String driverVersion,
        int maxConnections,
        boolean readOnly,
        boolean supportsTransactions,
        List<String> sqlKeywords
    ) {}

    public DatabaseInfoToolProvider(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a specification for the database information tool.
     * This tool retrieves metadata about the connected database including product name,
     * version, and supported features.
     *
     * @return A synchronous tool specification for the MCP framework
     */
    public McpServerFeatures.SyncToolSpecification getDatabaseInfoTool() {
        // Create the Tool record
        var tool = new McpSchema.Tool(
            "getDatabaseInfo",
             "Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.", 
             inputSchema
        );

        return new McpServerFeatures.SyncToolSpecification(tool, this::handleGetDatabaseInfo);
    }

    /**
     * Handles calls to the getDatabaseInfo tool.
     * Retrieves database metadata and formats it as a JSON response.
     *
     * @param exchange The MCP server exchange for communication
     * @param args Tool arguments (empty for this tool)
     * @return The result containing database information as JSON
     */
    private McpSchema.CallToolResult handleGetDatabaseInfo(McpSyncServerExchange exchange, Map<String, Object> args) {
        exchange.loggingNotification(LoggingMessageNotification.builder()
            .data("Getting database info...")
            .level(LoggingLevel.INFO)
            .build());
        try (var conn = dataSource.getConnection()) {
            var metaData = conn.getMetaData();    
            var dbInfo = collectDatabaseMetaData(metaData);

            var json = objectMapper.writeValueAsString(dbInfo);
            return new McpSchema.CallToolResult(List.of(new TextContent(json)), false); 
        } catch (Exception e) {
            if (e instanceof SQLException sqlException) {
                // Special handling for SQL exceptions
                exchange.loggingNotification(LoggingMessageNotification.builder()
                    .data("Database error: " + sqlException.getSQLState() + " - " + sqlException.getMessage())
                    .level(LoggingLevel.ERROR)
                    .build());
            } else {
                // General exception handling
                exchange.loggingNotification(LoggingMessageNotification.builder()
                    .data("Error getting database info: " + e.getMessage())
                    .level(LoggingLevel.ERROR)
                    .build());
            }
            String errorMessage = """
                    {"error": "Failed to retrieve database metadata.", "message": "%s"}
                    """.formatted(e.getMessage());
            return new McpSchema.CallToolResult(List.of(new TextContent(errorMessage)), true);
        }
    }

    private DatabaseInfo collectDatabaseMetaData(DatabaseMetaData metaData) throws SQLException {
        return new DatabaseInfo(
            metaData.getDatabaseProductName(),
            metaData.getDatabaseProductVersion(),
            metaData.getDriverName(),
            metaData.getDriverVersion(),
            metaData.getMaxConnections(),
            metaData.isReadOnly(),
            metaData.supportsTransactions(),
            parseKeywords(metaData.getSQLKeywords())
        );
    }

    private List<String> parseKeywords(String keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
    }
}
