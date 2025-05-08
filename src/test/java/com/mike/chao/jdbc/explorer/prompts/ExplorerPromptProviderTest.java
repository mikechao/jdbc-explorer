package com.mike.chao.jdbc.explorer.prompts;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExplorerPromptProviderTest {

    @Test
    void testCreatePromptAndHandler() {
        ExplorerPromptProvider provider = new ExplorerPromptProvider();
        McpServerFeatures.SyncPromptSpecification spec = provider.createPrompt();

        assertNotNull(spec);
        assertNotNull(spec.prompt());
        assertEquals("data-explorer", spec.prompt().name());
        assertEquals("Explores the connected database and create a dashboard.", spec.prompt().description());

        // Mock exchange and request
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        GetPromptRequest request = mock(GetPromptRequest.class);

        // Call the handler
        GetPromptResult result = spec.promptHandler().apply(exchange, request);

        assertNotNull(result);
        assertEquals("Explores the connected database and create a dashboard.", result.description());
        List<?> messages = result.messages();
        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof PromptMessage);

        PromptMessage msg = (PromptMessage) messages.get(0);
        assertEquals(Role.USER, msg.role());
        assertTrue(msg.content() instanceof TextContent);
        String text = ((TextContent) msg.content()).text();
        assertTrue(text.contains("You are an AI Business data analyst."));
    }
}