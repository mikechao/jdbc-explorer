package com.mike.chao.jdbc.explorer.config;

import com.mike.chao.jdbc.explorer.resources.BusinessInsights;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceConfigTest {

    @Mock
    private BusinessInsights mockBusinessInsights;

    @InjectMocks
    private ResourceConfig resourceConfig;

    @Mock
    private McpSyncServerExchange mockExchange;

    @Mock
    private McpSchema.ReadResourceRequest mockReadResourceRequest;

    private McpServerFeatures.SyncResourceSpecification resourceSpec;
    private McpSchema.Resource mcpResource;
    private BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> readHandler;

    @BeforeEach
    void setUp() {
        // Call the @Bean method to get the specification
        List<McpServerFeatures.SyncResourceSpecification> specs = resourceConfig.resources(mockBusinessInsights);
        assertNotNull(specs);
        assertEquals(1, specs.size());
        resourceSpec = specs.get(0);
        mcpResource = resourceSpec.resource();
        readHandler = resourceSpec.readHandler();
    }

    @Test
    void testResourceProperties() {
        assertNotNull(mcpResource, "McpSchema.Resource should not be null.");
        assertEquals("memo://insights", mcpResource.uri().toString(), "Resource URI should match.");
        assertEquals("Business Insights", mcpResource.name(), "Resource name should match.");
        assertEquals("Business Insights discover during data analysis", mcpResource.description(), "Resource description should match.");
        assertEquals("text/plain", mcpResource.mimeType(), "Resource media type should match.");
    }

    @Test
    void testReadHandler_knownUri_success() {
        String expectedInsights = "These are the insights.";
        URI knownUri = URI.create("memo://insights/some/path"); // startsWith "memo://insights"

        when(mockReadResourceRequest.uri()).thenReturn(knownUri.toString());
        when(mockBusinessInsights.getInsights()).thenReturn(expectedInsights);

        McpSchema.ReadResourceResult result = readHandler.apply(mockExchange, mockReadResourceRequest);

        assertNotNull(result, "ReadResourceResult should not be null.");
        assertNotNull(result.contents(), "Contents list should not be null.");
        assertEquals(1, result.contents().size(), "Should contain one content item.");

        assertTrue(result.contents().get(0) instanceof McpSchema.TextResourceContents, "Content should be TextResourceContents.");
        McpSchema.TextResourceContents textContent = (McpSchema.TextResourceContents) result.contents().get(0);

        assertEquals(knownUri.toString(), textContent.uri(), "Content URI should match request URI.");
        assertEquals("text/plain", textContent.mimeType(), "Content media type should be text/plain.");
        assertEquals(expectedInsights, textContent.text(), "Content text should match insights from BusinessInsights.");

        verify(mockBusinessInsights, times(1)).getInsights();
    }

    @Test
    void testReadHandler_unknownUri() {
        URI unknownUri = URI.create("memo://other/resource");

        when(mockReadResourceRequest.uri()).thenReturn(unknownUri.toString());

        McpSchema.ReadResourceResult result = readHandler.apply(mockExchange, mockReadResourceRequest);

        assertNotNull(result, "ReadResourceResult should not be null.");
        assertNotNull(result.contents(), "Contents list should not be null.");
        assertEquals(1, result.contents().size(), "Should contain one content item for error.");

        assertTrue(result.contents().get(0) instanceof McpSchema.TextResourceContents, "Error content should be TextResourceContents.");
        McpSchema.TextResourceContents textContent = (McpSchema.TextResourceContents) result.contents().get(0);

        assertEquals(unknownUri.toString(), textContent.uri(), "Error content URI should match request URI.");
        assertEquals("text/plain", textContent.mimeType(), "Error content media type should be text/plain.");
        assertEquals("Unknown resource uri", textContent.text(), "Error message should be 'Unknown resource uri'.");

        verify(mockBusinessInsights, never()).getInsights(); // Should not be called for unknown URI
    }

    @Test
    void testResourcesBean_returnsCorrectSpecification() {
        // This is implicitly tested by setUp, but an explicit assertion can be here
        List<McpServerFeatures.SyncResourceSpecification> specs = resourceConfig.resources(mockBusinessInsights);
        assertNotNull(specs);
        assertEquals(1, specs.size());
    }
}