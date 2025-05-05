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

    @Tool(name = "getTableNames", description = "Get all table names from the database")
    public List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("getTableNames")
                    .description("Get all table names from the database")
                    .build();
            throw new ToolExecutionException(toolDefinition, e);
        }
        return tableNames;
    }

    @Tool(name = "getDatabaseInfo", description = "Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.")
    public String getDatabaseInfo() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, String> info = new HashMap<>();

            info.put("database_product_name", metaData.getDatabaseProductName());
            info.put("database_product_version", metaData.getDatabaseProductVersion());
            info.put("driver_name", metaData.getDriverName());
            info.put("driver_version", metaData.getDriverVersion());
            info.put("max_connections", String.valueOf(metaData.getMaxConnections()));
            info.put("read_only", String.valueOf(metaData.isReadOnly()));
            info.put("supports_transactions", String.valueOf(metaData.supportsTransactions()));
            info.put("sql_keywords", metaData.getSQLKeywords());
           
            return objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("getDatabaseInfo")
                    .description("Get information about the database. Run this before anything else to know the SQL dialect, keywords etc.")
                    .build();
            throw new ToolExecutionException(toolDefinition, e);
        }
    }

    @Tool(name = "describeTable", description = "Describe a table in the database, including column information, primary keys, foreign keys, and indexes.")
    public Map<String, Object> describeTable(@ToolParam(description = "Catalog Name", required = false) String catalog,
        @ToolParam(description = "Schema Name", required = false) String schema,
        @ToolParam(description = "Name of the table to get description for") String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("table", tableName);
            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
                List<Map<String, String>> columns = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> column = new HashMap<>();
                    column.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
                    column.put("DATA_TYPE", rs.getString("DATA_TYPE"));
                    column.put("TYPE_NAME", rs.getString("TYPE_NAME"));
                    column.put("COLUMN_SIZE", rs.getString("COLUMN_SIZE"));
                    column.put("NULLABLE", rs.getString("NULLABLE"));
                    columns.add(column);
                }
                tableInfo.put("columns", columns);
            }

            // Get primary keys
            try (ResultSet pk = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                List<String> primaryKeys = new ArrayList<>();
                while (pk.next()) {
                    primaryKeys.add(pk.getString("COLUMN_NAME"));
                }
                tableInfo.put("primary_keys", primaryKeys);
            }

            // Get foreign keys
            try (ResultSet fk = metaData.getImportedKeys(catalog, schema, tableName)) {
                List<Map<String, String>> foreignKeys = new ArrayList<>();
                while (fk.next()) {
                    Map<String, String> fkInfo = new HashMap<>();
                    fkInfo.put("fk_column", fk.getString("FKCOLUMN_NAME"));
                    fkInfo.put("pk_table", fk.getString("PKTABLE_NAME"));
                    fkInfo.put("pk_column", fk.getString("PKCOLUMN_NAME"));
                    foreignKeys.add(fkInfo);
                }
                tableInfo.put("foreign_keys", foreignKeys);
            }

            // Get indexes
            try (ResultSet idx = metaData.getIndexInfo(catalog, schema, tableName, false, false)) {
                List<Map<String, String>> indexes = new ArrayList<>();
                while (idx.next()) {
                    Map<String, String> idxInfo = new HashMap<>();
                    idxInfo.put("index_name", idx.getString("INDEX_NAME"));
                    idxInfo.put("column_name", idx.getString("COLUMN_NAME"));
                    idxInfo.put("unique", String.valueOf(!idx.getBoolean("NON_UNIQUE")));
                    indexes.add(idxInfo);
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
