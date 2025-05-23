package com.mike.chao.jdbc.explorer.tools;

import com.mike.chao.jdbc.explorer.resources.BusinessInsights;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BusinessInsightsToolProviderTest {

    private BusinessInsights mockBusinessInsights;
    private BusinessInsightsToolProvider toolProvider;
    private McpSyncServerExchange mockExchange;

    @BeforeEach
    void setUp() {
        mockBusinessInsights = mock(BusinessInsights.class);
        toolProvider = new BusinessInsightsToolProvider(mockBusinessInsights);
        mockExchange = mock(McpSyncServerExchange.class);
    }

    @Test
    void testGetAddBusinessInsightsTool_toolDefinition() {
        McpServerFeatures.SyncToolSpecification spec = toolProvider.getAddBusinessInsightsTool();

        assertNotNull(spec);
        McpSchema.Tool tool = spec.tool();
        assertNotNull(tool);
        assertEquals("addBusinessInsight", tool.name());
        assertEquals("Add a business insight discovered during data analysis to the memo.", tool.description());
        assertNotNull(tool.inputSchema()); // Verifying schema is present
    }

    @Test
    void testGetAddBusinessInsightsTool_call_success() {
        McpServerFeatures.SyncToolSpecification spec = toolProvider.getAddBusinessInsightsTool();
        String testInsight = "This is a test insight.";
        Map<String, Object> args = Map.of("insights", testInsight);

        CallToolResult result = spec.call().apply(mockExchange, args);

        assertNotNull(result);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("Business insight added successfully.", ((TextContent) result.content().get(0)).text());

        verify(mockBusinessInsights, times(1)).addInsight(testInsight);
        ArgumentCaptor<LoggingMessageNotification> loggingCaptor = ArgumentCaptor.forClass(LoggingMessageNotification.class);
        verify(mockExchange, times(2)).loggingNotification(loggingCaptor.capture());
        assertEquals("Business insight added successfully.", loggingCaptor.getValue().data());
        assertEquals(LoggingLevel.INFO, loggingCaptor.getValue().level());
    }

    @Test
    void testGetAddBusinessInsightsTool_call_nullInsight() {
        McpServerFeatures.SyncToolSpecification spec = toolProvider.getAddBusinessInsightsTool();
        Map<String, Object> args = new HashMap<>();
        args.put("insights", null);

        CallToolResult result = spec.call().apply(mockExchange, args);

        assertNotNull(result);
        assertTrue(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("""
                {"error": "NullInput", "message": "Business insight cannot be null"}
                """, ((TextContent) result.content().get(0)).text());

        verify(mockBusinessInsights, never()).addInsight(anyString());
        ArgumentCaptor<LoggingMessageNotification> loggingCaptor = ArgumentCaptor.forClass(LoggingMessageNotification.class);
        verify(mockExchange, times(2)).loggingNotification(loggingCaptor.capture());
        assertEquals("Error: Business insight input was null.", loggingCaptor.getValue().data());
        assertEquals(LoggingLevel.ERROR, loggingCaptor.getValue().level());
    }

    @Test
    void testGetAddBusinessInsightsTool_call_insightNotString() {
        McpServerFeatures.SyncToolSpecification spec = toolProvider.getAddBusinessInsightsTool();
        Map<String, Object> args = Map.of("insights", 123); // Not a string

        CallToolResult result = spec.call().apply(mockExchange, args);

        assertNotNull(result);
        assertTrue(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("""
                {"error": "InvalidInputType", "message": "Invalid input type for insight. Expected String.", "receivedType": "Integer"}
                """, ((TextContent) result.content().get(0)).text());

        verify(mockBusinessInsights, never()).addInsight(anyString());
        ArgumentCaptor<LoggingMessageNotification> loggingCaptor = ArgumentCaptor.forClass(LoggingMessageNotification.class);
        verify(mockExchange, times(2)).loggingNotification(loggingCaptor.capture());
        assertEquals("""
                Error: {"error": "InvalidInputType", "message": "Invalid input type for insight. Expected String.", "receivedType": "Integer"}
                """, loggingCaptor.getValue().data());
        assertEquals(LoggingLevel.ERROR, loggingCaptor.getValue().level());
    }

    @Test
    void testGetAddBusinessInsightsTool_call_emptyArgs() {
        McpServerFeatures.SyncToolSpecification spec = toolProvider.getAddBusinessInsightsTool();
        Map<String, Object> args = Map.of(); // "insights" key is missing

        CallToolResult result = spec.call().apply(mockExchange, args);

        assertNotNull(result);
        assertTrue(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("""
                {"error": "NoArguments", "message": "No arguments provided"}
                """, ((TextContent) result.content().get(0)).text());

        verify(mockBusinessInsights, never()).addInsight(anyString());
        ArgumentCaptor<LoggingMessageNotification> loggingCaptor = ArgumentCaptor.forClass(LoggingMessageNotification.class);
        verify(mockExchange, times(1)).loggingNotification(loggingCaptor.capture());
        assertEquals("Error: No arguments provided.", loggingCaptor.getValue().data());
        assertEquals(LoggingLevel.ERROR, loggingCaptor.getValue().level());
    }

    @Test
    void testGetAddBusinessInsightsTool_call_nullValue() {
        McpServerFeatures.SyncToolSpecification spec = toolProvider.getAddBusinessInsightsTool();
        Map<String, Object> args = new HashMap<>();
        args.put("insights", null); // Null value for insights

        CallToolResult result = spec.call().apply(mockExchange, args);

        assertNotNull(result);
        assertTrue(result.isError());
        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0) instanceof TextContent);
        assertEquals("""
                {"error": "NullInput", "message": "Business insight cannot be null"}
                """, ((TextContent) result.content().get(0)).text());

        verify(mockBusinessInsights, never()).addInsight(anyString());
        ArgumentCaptor<LoggingMessageNotification> loggingCaptor = ArgumentCaptor.forClass(LoggingMessageNotification.class);
        verify(mockExchange, times(2)).loggingNotification(loggingCaptor.capture());
        assertEquals("Error: Business insight input was null.", loggingCaptor.getValue().data());
        assertEquals(LoggingLevel.ERROR, loggingCaptor.getValue().level());
    }
}