package com.mike.chao.jdbc.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service
public class ExplorerService {

    private final DataSource dataSource;
    private final List<String> businessInsights;

    @Autowired
    public ExplorerService(DataSource dataSource,  @Qualifier("businessInsights") List<String> businessInsights) {
        this.dataSource = dataSource;
        this.businessInsights = businessInsights;
    }

    @Tool(name = "addBusinessInsight", description = "Add a business insight discovered during data analysis to the memo.")
    public void addBusinessInsight(@ToolParam(description = "business insight discovered during data analysis", required = true)String insight) {
        businessInsights.add(insight);
    }

    @Tool(name = "executeQuery", description = "Execute a SQL query and return the results")
    public List<Map<String, Object>> executeQuery(@ToolParam(description = "SQL query to execute", required = true) String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            try (ResultSet rs = conn.createStatement().executeQuery(query)) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = rsmd.getColumnName(i);
                        Object value = rs.getObject(i);
                        if (value != null) {
                            row.put(columnName, value);
                        } else {
                            row.put(columnName, null); // Handle null values explicitly
                        }
                    }
                    results.add(row);
                }
            }
        } catch (Exception e) {
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("executeQuery")
                    .description("Execute a SQL query and return the results")
                    .build();
            throw new ToolExecutionException(toolDefinition, e);
        }
        return results;
    }

    @Tool(name = "getTableNames", description = "Get all table names from the database, including type, schema, and remarks")
    public List<Map<String, Object>> getTableNames() {
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", null)) {
                while (rs.next()) {
                    String tableType = rs.getString("TABLE_TYPE");
                    if (!"TABLE".equalsIgnoreCase(tableType)) {
                        continue; // Skip system tables and views
                    }
                    Map<String, Object> table = new HashMap<>();
                    table.put("tableName", rs.getString("TABLE_NAME"));
                    table.put("tableType", tableType);
                    table.put("remarks", rs.getString("REMARKS"));
                    table.put("schema", rs.getString("TABLE_SCHEM"));
                    table.put("catalog", rs.getString("TABLE_CAT"));
                    tables.add(table);
                }
            }
        } catch (Exception e) {
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("getTableNames")
                    .description("Get all table names from the database, including type, schema, and remarks")
                    .build();
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
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("describeTable")
                    .description("Describe a table in the database")
                    .build();
            throw new ToolExecutionException(toolDefinition, e);
        }
    }
}
