package com.mike.chao.jdbc.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.ai.tool.annotation.Tool;
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
}
