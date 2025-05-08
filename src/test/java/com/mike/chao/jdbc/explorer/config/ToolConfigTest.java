package com.mike.chao.jdbc.explorer.config;

import com.mike.chao.jdbc.explorer.ExplorerService;
import com.mike.chao.jdbc.explorer.tools.BusinessInsightsToolProvider;
import com.mike.chao.jdbc.explorer.tools.DatabaseInfoToolProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolConfigTest {

    @Mock
    private ExplorerService mockExplorerService;

    @Mock
    private DatabaseInfoToolProvider mockDatabaseInfoToolProvider;

    @Mock
    private BusinessInsightsToolProvider mockBusinessInsightsToolProvider;

    @InjectMocks
    private ToolConfig toolConfig;

    // Mocks for static method calls
    private MockedStatic<ToolCallbacks> mockedToolCallbacks;
    private MockedStatic<McpToolUtils> mockedMcpToolUtils;

    @Mock
    private McpServerFeatures.SyncToolSpecification mockExplorerServiceSpec;
    @Mock
    private McpServerFeatures.SyncToolSpecification mockDbInfoSpec;
    @Mock
    private McpServerFeatures.SyncToolSpecification mockBusinessInsightsSpec;
    @Mock
    private ToolCallback mockExplorerToolCallback;


    @BeforeEach
    void setUp() {
        // Initialize static mocks
        mockedToolCallbacks = Mockito.mockStatic(ToolCallbacks.class);
        mockedMcpToolUtils = Mockito.mockStatic(McpToolUtils.class);

        // Define behavior for static method ToolCallbacks.from()
        // It should return ToolCallback[]
        ToolCallback[] explorerToolCallbacksArray = {mockExplorerToolCallback}; // Create an array
        mockedToolCallbacks.when(() -> ToolCallbacks.from(mockExplorerService)).thenReturn(explorerToolCallbacksArray); // Return the array

        // Define behavior for static method McpToolUtils.toSyncToolSpecification()
        // This method expects a List<ToolCallback> as input
        List<ToolCallback> explorerToolCallbacksList = List.of(explorerToolCallbacksArray); // Convert array to List for this call
        List<McpServerFeatures.SyncToolSpecification> explorerServiceSpecs = new ArrayList<>(Collections.singletonList(mockExplorerServiceSpec));
        
        // Ensure the argument matcher matches the list created from ToolCallbacks.from()
        // The actual code does: List.of(ToolCallbacks.from(explorerService))
        // So we need to mock based on what the actual code passes to toSyncToolSpecification
        mockedMcpToolUtils.when(() -> McpToolUtils.toSyncToolSpecification(explorerToolCallbacksList)).thenReturn(explorerServiceSpecs);


        // Define behavior for provider methods
        when(mockDatabaseInfoToolProvider.getDatabaseInfoTool()).thenReturn(mockDbInfoSpec);
        when(mockBusinessInsightsToolProvider.getAddBusinessInsightsTool()).thenReturn(mockBusinessInsightsSpec);
    }

    @AfterEach
    void tearDown() {
        // Close static mocks to avoid interference between tests
        mockedToolCallbacks.close();
        mockedMcpToolUtils.close();
    }

    @Test
    void testToolsBeanCreation() {
        // Call the @Bean method
        List<McpServerFeatures.SyncToolSpecification> toolsList = toolConfig.tools(
                mockExplorerService,
                mockDatabaseInfoToolProvider,
                mockBusinessInsightsToolProvider
        );

        // Verify static method calls
        mockedToolCallbacks.verify(() -> ToolCallbacks.from(mockExplorerService), times(1));
        
        // The argument to toSyncToolSpecification should be the list created from the array
        // In the actual code: List.of(ToolCallbacks.from(explorerService))
        ToolCallback[] expectedArray = {mockExplorerToolCallback};
        mockedMcpToolUtils.verify(() -> McpToolUtils.toSyncToolSpecification(List.of(expectedArray)), times(1));


        // Verify provider method calls
        verify(mockDatabaseInfoToolProvider, times(1)).getDatabaseInfoTool();
        verify(mockBusinessInsightsToolProvider, times(1)).getAddBusinessInsightsTool();

        // Assertions on the returned list
        assertNotNull(toolsList, "The list of tools should not be null.");
        // Expected size: 1 from ExplorerService + 1 from DatabaseInfoToolProvider + 1 from BusinessInsightsToolProvider
        assertEquals(3, toolsList.size(), "The list should contain three tool specifications.");

        // Check if the list contains the expected mocked specifications
        assertTrue(toolsList.contains(mockExplorerServiceSpec), "List should contain spec from ExplorerService.");
        assertTrue(toolsList.contains(mockDbInfoSpec), "List should contain spec from DatabaseInfoToolProvider.");
        assertTrue(toolsList.contains(mockBusinessInsightsSpec), "List should contain spec from BusinessInsightsToolProvider.");

        // Check the order if it's important (it is, due to add())
        assertSame(mockExplorerServiceSpec, toolsList.get(0), "First element should be from ExplorerService.");
        assertSame(mockDbInfoSpec, toolsList.get(1), "Second element should be from DatabaseInfoToolProvider.");
        assertSame(mockBusinessInsightsSpec, toolsList.get(2), "Third element should be from BusinessInsightsToolProvider.");
    }
}