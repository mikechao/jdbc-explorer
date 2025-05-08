package com.mike.chao.jdbc.explorer.config;

import com.mike.chao.jdbc.explorer.prompts.ExplorerPromptProvider;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptConfigTest {

    @Mock
    private ExplorerPromptProvider mockExplorerPromptProvider;

    @Mock
    private McpServerFeatures.SyncPromptSpecification mockSyncPromptSpecification;

    @InjectMocks
    private PromptConfig promptConfig;

    @BeforeEach
    void setUp() {
        // Configure the mock provider to return the mock specification
        when(mockExplorerPromptProvider.createPrompt()).thenReturn(mockSyncPromptSpecification);
    }

    @Test
    void testPromptsBeanCreation() {
        // Call the @Bean method
        List<McpServerFeatures.SyncPromptSpecification> promptsList = promptConfig.prompts();

        // Verify that the explorerPromptProvider.createPrompt() method was called exactly once
        verify(mockExplorerPromptProvider, times(1)).createPrompt();

        // Assertions on the returned list
        assertNotNull(promptsList, "The list of prompts should not be null.");
        assertEquals(1, promptsList.size(), "The list should contain exactly one prompt specification.");
        assertSame(mockSyncPromptSpecification, promptsList.get(0), "The prompt specification in the list should be the one returned by the provider.");
    }
}