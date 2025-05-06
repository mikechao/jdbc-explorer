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

## Resources 🗂️

The server contains 1 resource.

- **Business Insights**

    - Contains the list of business insights that the LLM came up with during data analysis.
    - `uri`: "memo://insights"

## Supported JDBC variants

This server currently supports the following databases.

| Database |
|----------|
|sqlite|
|PostgreSQL|
|Oracle|
|h2|
|MySQL|

## Example Databases

**Netflix Movies**

Sample movie data based on Netflix catalog
[Netflix sample DB](https://github.com/lerocha/netflixdb)

**Northwind**

Classic Microsoft sample database with customers, orders, products etc.

[Northwind Sqlite](https://github.com/jpwhite3/northwind-SQLite3)

**Chinook**

Sample music store data including artists, albums, tracks, invoices etc.

[Chinook Database](https://github.com/lerocha/chinook-database)

## Usage with Claude Desktop

### From jar

1. Clone the repo
2. Build the jar with maven
```bash
mvn clean package
```

Add this to your `claude_desktop_config.json`:

#### Sqlite
```json
{
    "mcpServers": {
		  "jdbc-explorer": {
			"command": "java",
			"args": [
			  "-jar",
			  "C:\\\\mcp\\\\jdbc-explorer\\\\target\\\\jdbc.explorer-0.0.1-SNAPSHOT.jar",
			  "--db.url=jdbc:sqlite:C:\\\\mcp\\\\jdbc-explorer\\\\netflixdb.sqlite"
			]
		  }
	}
}
```

#### Database with username and password
```json
{
    "mcpServers": {
		  "jdbc-explorer": {
			"command": "java",
			"args": [
			  "-jar",
			  "C:\\\\mcp\\\\jdbc-explorer\\\\target\\\\jdbc.explorer-0.0.1-SNAPSHOT.jar",
			  "--db.url=jdbc:postgresql://localhost:5432/chinook",
			  "--db.username=dbuser",
			  "--db.password=dbpassword"
			]
		  }
	}
}
```

### From Docker image

1. Clone the repo
2. Build the docker image
```bash
docker build -t jdbc-explorer .
```

Add this to your `claude_desktop_config.json`:

#### Database with username and password
```json
{
    "mcpServers": {
		  "jdbc-explorer": {
			"command": "docker",
			"args": [
			  "run",
			  "-i",
			  "--rm",
			  "-e",
			  "DB_URL=jdbc:postgresql://host.docker.internal:5432/chinook",
			  "-e",
			  "DB_USERNAME=dbuser",
			  "-e",
			  "DB_PASSWORD=dbpassword",
			  "jdbc-explorer"
			]
		  }
	}
}
```



## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This MCP server is licensed under the MIT License. This means you are free to use, modify, and distribute the software, subject to the terms and conditions of the MIT License. For more details, please see the LICENSE file in the project repository.