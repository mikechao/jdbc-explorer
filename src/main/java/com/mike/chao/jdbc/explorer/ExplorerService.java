package com.mike.chao.jdbc.explorer;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
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

@Service
public class ExplorerService {

    private final DataSource dataSource;

    @Autowired
    public ExplorerService(DataSource dataSource) {
        this.dataSource = dataSource;
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
        } catch (SQLException e) {
            ToolDefinition toolDefinition = ToolDefinition.builder()
                    .name("getTableNames")
                    .description("Get all table names from the database")
                    .build();
            throw new ToolExecutionException(toolDefinition, e);
        }
        return tableNames;
    }
}
