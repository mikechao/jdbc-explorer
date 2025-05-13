package com.mike.chao.jdbc.explorer.tools;



import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class DatabaseInfoToolProviderTest {

    private DatabaseInfoToolProvider provider;

    @BeforeEach
    void setUp() {
        DataSource dataSource = createH2DataSource();
        provider = new DatabaseInfoToolProvider(dataSource, new ObjectMapper());
    }

    @Test
    void testGetDatabaseInfoToolReturnsExpectedInfo() throws Exception {
        McpServerFeatures.SyncToolSpecification spec = provider.getDatabaseInfoTool();
        // Use a dummy exchange (can be a mock or simple implementation)
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        CallToolResult result = spec.call().apply(exchange, Map.of());
        assertNotNull(result);
        assertFalse(result.isError());
        List<TextContent> contents = result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .toList();
        assertFalse(contents.isEmpty());
        String json = contents.get(0).text();
        assertTrue(json.contains("databaseProductName"));
        assertTrue(json.contains("H2"));
    }

    @Test
    void testGetDatabaseInfoToolHandlesException() throws Exception {
        // Mock DataSource to throw SQLException
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Simulated DB error"));

        DatabaseInfoToolProvider provider = new DatabaseInfoToolProvider(dataSource, new ObjectMapper());
        McpServerFeatures.SyncToolSpecification spec = provider.getDatabaseInfoTool();
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);

        CallToolResult result = spec.call().apply(exchange, Map.of());
        assertNotNull(result);
        assertTrue(result.isError());
        List<TextContent> contents = result.content().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .toList();
        assertFalse(contents.isEmpty());
        String text = contents.get(0).text();
        assertTrue(text.contains("Simulated DB error"));

        // Verify that loggingNotification was called with error level
        verify(exchange, atLeastOnce()).loggingNotification(argThat(notification ->
                notification.level().name().equalsIgnoreCase("ERROR") &&
                notification.data().contains("Simulated DB error")
        ));
    }

    // Helper to create an H2 in-memory DataSource
    private DataSource createH2DataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

}