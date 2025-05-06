# JDBC Explorer

A [Model Context Protocol](https://modelcontextprotocol.io/introduction) server for connecting LLM to databases via JDBC. This server is implemented using the [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) framework. The server exposes server tools, a prompt and resources to interact with the connected database.

## Tools 🛠

The server contains the following tools.

- **addBusinessInsight** 

    - Adds business insights discovered during data analysis to the "Business Insights" resource. Usually executed as part of the prompt "data-explorer"
    - Inputs:
        - `insight` (String): business insight discovered during data analysis 

- **executeQuery**

    - Executes a SQL query against the connected database, returning the results
    - Inputs:
        - `query` (string): the SQL query to be executed

- **getTableNames**

    - Gets the table names, including type, schema, and remarks
    - Inputs: none

- **describeTable**
    
    - Describe a table in the database including column information, primary keys, foreign keys, and indexes.
    - Inputs:
        - `catalog` (string, optional): Catalog Name
        - `schema` (string, optional): Schema Name
        - `tableName` (string): Name of the table to get description for

- **getDatabaseInfo**

    - Get information about the database including SQL dialect, keywords, database product name, etc.
    - Inputs: none

## Prompts 📄

The server contains 1 prompt.

- **data-explorer**

This prompt helps the user explore the data in their databases. It should present the user with a choice of dashboards that the LLM can create. The LLM will then execute the necessary queries and create the selected dashboard using an artifact.

The prompt result in Claude Desktop

<a href="https://mikechao.github.io/images/jdbc-explorer-prompt.webp" target="_blank" rel="noopener noreferrer">
<img width="380" height="200" src="https://mikechao.github.io/images/jdbc-explorer-prompt.webp" alt="claude desktop example" />
</a>

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This MCP server is licensed under the MIT License. This means you are free to use, modify, and distribute the software, subject to the terms and conditions of the MIT License. For more details, please see the LICENSE file in the project repository.