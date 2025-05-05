package com.mike.chao.jdbc.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExplorerService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExplorerService(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
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

    @Tool(name = "getDatabaseInfo", description = "Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.")
    public Map<String, Object> getDatabaseInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, Object> info = new HashMap<>();

            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("maxConnections", metaData.getMaxConnections());
            info.put("readOnly", metaData.isReadOnly());
            info.put("supportsTransactions", metaData.supportsTransactions());

            // Split SQL keywords into a list
            String keywords = metaData.getSQLKeywords();
            List<String> sqlKeywords = new ArrayList<>();
            if (keywords != null && !keywords.isEmpty()) {
                for (String kw : keywords.split(",")) {
                    sqlKeywords.add(kw.trim());
                }
            }
            info.put("sqlKeywords", sqlKeywords);

            return info;
        } catch (Exception e) {
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("getDatabaseInfo")
                    .description("Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.")
                    .build();
            throw new ToolExecutionException(toolDefinition, e);
        }
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
