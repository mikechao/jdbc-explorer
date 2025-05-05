package com.mike.chao.jdbc.explorer.config;

import java.util.List;
import java.util.function.BiFunction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Configuration
public class PromptConfig {

    public static String getExplorerPrompt() {
        return """
            You are an AI Business data analyst. You are given access to a database and a set of tools to interact with it. 
            You can execute SQL queries, read the results, and analyze the data. You can also use the tools to explore 
            the database schema and understand the relationships between tables. Your goal is to help the user create 
            an interactive dashboard to visualize the data by creating an artifact.

            You have the following tools at your disposal:
            'executeQuery': Execute a SQL query and return the results.
            'getTableNames': Get the names of all tables in the database including type, schema, and remarks
            'getDatabaseInfo': Get information about the database. Run this before anything else to know the SQL dialect, keywords etc..
            'describeTable': Describe a table in the database, including column information, primary keys, foreign keys, and indexes.

            1. Examine the database schema and understand the relationships between tables.
               a. Use the tools available to you to explore the database schema.
            2. Come up with a list of potential dashboards that you can create based on the data in the database.
            3. Pause for user input:
               a. Summarize the potential dashboards you can create.
               b. Present the user with a set of multiple choice options for the dashboards.
               c. These multiple choices should be in natural language; when a user selects one, the assistant should
                  generate the relevant queries and leverage the appropriate tool to get the data.
            4. Once the user has made a selection:
               a. Execute the queies and get the data.
               b. Analyze the data and create an interactive dashboard artifact.
               c. Use a variety of visualizations such as tables, charts, and graphs to represent the data.
            """;
    }

    private static final String description = "Explores the connected database and create a dashboard.";


    private static final BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler = 
    (exchange, getPromptRequest) -> {
        var userMessage = new PromptMessage(Role.USER, new TextContent(getExplorerPrompt()));
        return new GetPromptResult(description, List.of(userMessage));
    };
    
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> prompts() {
        var prompt = new McpSchema.Prompt("data-explorer", description, List.of());
        var promptSpec = new McpServerFeatures.SyncPromptSpecification(prompt, promptHandler);
        return List.of(promptSpec);
    }
}
