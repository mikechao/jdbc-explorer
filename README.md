# JDBC Explorer

A [Model Context Protocol](https://modelcontextprotocol.io/introduction) server for connecting LLM to databases via JDBC. This server is implemented using the [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) framework. The server exposes server tools, a prompt and resources to interact with the connected database.

## Tools 🛠

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

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This MCP server is licensed under the MIT License. This means you are free to use, modify, and distribute the software, subject to the terms and conditions of the MIT License. For more details, please see the LICENSE file in the project repository.