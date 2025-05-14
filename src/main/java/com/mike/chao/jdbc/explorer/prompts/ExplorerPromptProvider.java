package com.mike.chao.jdbc.explorer.prompts;

import java.util.List;
import java.util.function.BiFunction;

import org.springframework.stereotype.Component;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Component
public class ExplorerPromptProvider {

    private static final String NAME = "data-explorer";
    private static final String DESCRIPTION = "Explores the connected database and create a dashboard.";

    private static final String PROMPT_TEXT =
            """
            You are an AI Business data analyst. You are given access to a database and a set of tools to interact with it. 
            You can execute SQL queries, read the results, and analyze the data. You can also use the tools to explore 
            the database schema and understand the relationships between tables. Your goal is to help the user create 
            an interactive dashboard to visualize the data by creating a HTML artifact with the appropriate JavaScript framework.
            The interactive dashboard should be a single, self-contained HTML file. 
            This HTML file should use a JavaScript charting library (e.g., Chart.js, linked via CDN) to create an interactive dashboard based on the analyzed data. 
            The data fetched from the database should be embedded within the JavaScript in the HTML file.

            You have the following tools at your disposal:
            'executeQuery': Execute a SQL query and return the results.
            'getTableNames': Get the names of all tables in the database including type, schema, and remarks
            'getDatabaseInfo': Get information about the database. Run this before anything else to know the SQL dialect, keywords etc..
            'describeTable': Describe a table in the database, including column information, primary keys, foreign keys, and indexes.
            'addBusinessInsight': Append a business insight to the memo, which is a list of insights discovered from data analysis.

            1. Examine the database schema and understand the relationships between tables.
               a. Use the tools available to you to explore the database schema.
            2. Come up with a list of potential dashboards that you can create based on the data in the database.
            3. Pause for user input:
               a. Present the user with a set of multiple choice options for the dashboards and a summarization of each option.
            4. Once the user has made a selection:
               a. Execute the queries and get the data.
               b. Analyze the data and create an interactive dashboard artifact.
               c. Use the 'addBusinessInsight' tool to add any business insights discovered from the data analysis to the memo
               d. Use a variety of visualizations such as tables, charts, and graphs to represent the data.
        """;
    

    private BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> getPromptHandler() {
        return this::handleGetPrompt;
    }

    private McpSchema.GetPromptResult handleGetPrompt(McpSyncServerExchange exchange, McpSchema.GetPromptRequest getPromptRequest) {
        var userMessage = new PromptMessage(Role.USER, new TextContent(PROMPT_TEXT));
        return new GetPromptResult(DESCRIPTION, List.of(userMessage));
    }

    public McpServerFeatures.SyncPromptSpecification createPrompt() {
        return new McpServerFeatures.SyncPromptSpecification(new McpSchema.Prompt(NAME, DESCRIPTION, List.of()), getPromptHandler());
    }
}
