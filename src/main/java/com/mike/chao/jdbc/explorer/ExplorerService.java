package com.mike.chao.jdbc.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mike.chao.jdbc.explorer.data.TableInfo;

@Service
public class ExplorerService {

    private final DataSource dataSource;
    private final Logger logger = LoggerFactory.getLogger(ExplorerService.class);

    @Autowired
    public ExplorerService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Tool(name = "executeQuery", description = "Execute a SQL query and return the results")
    public List<Map<String, Object>> executeQuery(@ToolParam(description = "SQL query to execute", required = true) String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsmd.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        } catch (Exception e) {
            logger.error("Error executing query: {} message: {}", query, e.getMessage(), e);
            ToolDefinition toolDefinition = getToolDefinition("executeQuery");
            throw new ToolExecutionException(toolDefinition, e);
        }
        return results;
    }

    @Tool(name = "getTableNames", description = "Get all table names from the database, including type, schema, and remarks")
    public List<TableInfo> getTableNames() {
        List<TableInfo> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String[] types = {"TABLE"}; // Only include tables, exclude views and system tables
            try (ResultSet rs = metaData.getTables(null, null, "%", types)) {
                while (rs.next()) {
                    var table = new TableInfo(
                        rs.getString("TABLE_NAME"),
                        rs.getString("TABLE_TYPE"),
                        rs.getString("REMARKS"),
                        rs.getString("TABLE_SCHEM"),
                        rs.getString("TABLE_CAT")
                    );
                    tables.add(table);
                }
            }
        } catch (Exception e) {
            logger.error("Error getTableNames message: {}", e.getMessage(), e);
            ToolDefinition toolDefinition = getToolDefinition("getTableNames");
            throw new ToolExecutionException(toolDefinition, e);
        }
        return tables;
    }

    @Tool(name = "describeTable", description = "Describe a table in the database, including column information, primary keys, foreign keys, and indexes.")
    public Map<String, Object> describeTable(
        @ToolParam(description = "Catalog Name", required = false) String catalog,
        @ToolParam(description = "Schema Name", required = false) String schema,
        @ToolParam(description = "Name of the table to get description for") String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            // Check if the table exists
            try (ResultSet tables = metaData.getTables(catalog, schema, tableName, new String[] {"TABLE"})) {
                if (!tables.next()) {
                    throw new IllegalArgumentException("Table '" + tableName + "' does not exist in the database.");
                }
            }

            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("table", tableName);

            // Columns
            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
                List<Map<String, Object>> columns = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("name", rs.getString("COLUMN_NAME"));
                    column.put("type", rs.getString("TYPE_NAME"));
                    column.put("size", rs.getInt("COLUMN_SIZE"));
                    column.put("nullable", "1".equals(rs.getString("NULLABLE")));
                    columns.add(column);
                }
                tableInfo.put("columns", columns);
            }

            // Primary keys
            try (ResultSet pk = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                List<String> primaryKeys = new ArrayList<>();
                while (pk.next()) {
                    primaryKeys.add(pk.getString("COLUMN_NAME"));
                }
                tableInfo.put("primaryKeys", primaryKeys);
            }

            // Foreign keys
            try (ResultSet fk = metaData.getImportedKeys(catalog, schema, tableName)) {
                List<Map<String, Object>> foreignKeys = new ArrayList<>();
                while (fk.next()) {
                    Map<String, Object> fkInfo = new HashMap<>();
                    fkInfo.put("column", fk.getString("FKCOLUMN_NAME"));
                    fkInfo.put("referencesTable", fk.getString("PKTABLE_NAME"));
                    fkInfo.put("referencesColumn", fk.getString("PKCOLUMN_NAME"));
                    foreignKeys.add(fkInfo);
                }
                tableInfo.put("foreignKeys", foreignKeys);
            }

            // Indexes
            try (ResultSet idx = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
                List<Map<String, Object>> indexes = new ArrayList<>();
                while (idx.next()) {
                    String indexName = idx.getString("INDEX_NAME");
                    String columnName = idx.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) { // filter out statistics rows
                        Map<String, Object> idxInfo = new HashMap<>();
                        idxInfo.put("name", indexName);
                        idxInfo.put("column", columnName);
                        idxInfo.put("unique", !idx.getBoolean("NON_UNIQUE"));
                        indexes.add(idxInfo);
                    }
                }
                tableInfo.put("indexes", indexes);
            }
            return tableInfo;
        } catch (Exception e) {
            logger.error("Error describeTable for " + tableName + " message:" + e.getMessage(), e);
            ToolDefinition toolDefinition = getToolDefinition("describeTable");
            throw new ToolExecutionException(toolDefinition, e);
        }
    }

    private ToolDefinition getToolDefinition(String toolName) {
        List<ToolCallback> toolCallBacks = List.of(ToolCallbacks.from(this));
        Optional<ToolDefinition> toolDefinitionOptional = toolCallBacks.stream()
            .map(ToolCallback::getToolDefinition)
            .filter(definition -> definition.name().equals(toolName))
            .findFirst();
        return toolDefinitionOptional.orElse(getUnknownToolDefinition(toolName));
    }

    private ToolDefinition getUnknownToolDefinition(String toolName) {
        return ToolDefinition.builder()
            .name(toolName)
            .description("Tool not found")
            .inputSchema("""
                {
                    "$schema" : "https://json-schema.org/draft/2020-12/schema",
                    "type" : "object",
                    "properties" : { },
                    "required" : [ ],
                    "additionalProperties" : false
                }
            """)
            .build();
    }
}
