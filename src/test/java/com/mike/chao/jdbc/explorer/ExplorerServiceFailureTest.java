package com.mike.chao.jdbc.explorer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.ai.tool.execution.ToolExecutionException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplorerServiceFailureTest {

    @Mock
    private DataSource mockDataSource;
    @Mock
    private Connection mockConnection;
    @Mock
    private Statement mockStatement;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private ResultSetMetaData mockResultSetMetaData;
    @Mock
    private DatabaseMetaData mockDatabaseMetaData;
    @Mock
    private Logger mockLogger; // Mock the logger

    @InjectMocks
    private ExplorerService explorerService;

    @BeforeEach
    void setUp() throws SQLException {
        // Inject the mocked logger
        try {
            java.lang.reflect.Field loggerField = ExplorerService.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            loggerField.set(explorerService, mockLogger);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to inject mock logger", e);
        }

        // Common mock setup
        lenient().when(mockDataSource.getConnection()).thenReturn(mockConnection);
        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);
        lenient().when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        lenient().when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);
        lenient().when(mockConnection.getMetaData()).thenReturn(mockDatabaseMetaData);
    }

    // --- executeQuery Tests ---

    @Test
    void testExecuteQuery_sqlException() throws SQLException {
        String query = "SELECT * FROM non_existent_table";
        SQLException sqlEx = new SQLException("Table not found");
        when(mockDataSource.getConnection()).thenThrow(sqlEx);

        ToolExecutionException ex = assertThrows(ToolExecutionException.class, () -> {
            explorerService.executeQuery(query);
        });

        assertEquals(sqlEx, ex.getCause());
        verify(mockLogger).error(eq("Error executing query: " + query + " message:" + sqlEx.getMessage()), eq(sqlEx));
    }

    // --- getTableNames Tests ---

    @Test
    void testGetTableNames_sqlException() throws SQLException {
        SQLException sqlEx = new SQLException("DB metadata error");
        when(mockDataSource.getConnection()).thenThrow(sqlEx);

        ToolExecutionException ex = assertThrows(ToolExecutionException.class, () -> {
            explorerService.getTableNames();
        });

        assertEquals(sqlEx, ex.getCause());
        verify(mockLogger).error(eq("Error getTableNames message:" + sqlEx.getMessage()), eq(sqlEx));
    }

    // --- describeTable Tests ---
    @Test
    void testDescribeTable_tableNotFound() throws SQLException {
        String tableName = "non_existent_table";
        ResultSet mockTableNotFoundResultSet = mock(ResultSet.class);
        when(mockDatabaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"})).thenReturn(mockTableNotFoundResultSet);
        when(mockTableNotFoundResultSet.next()).thenReturn(false); // Table does not exist

        ToolExecutionException ex = assertThrows(ToolExecutionException.class, () -> {
            explorerService.describeTable(null, null, tableName);
        });

        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("Table '" + tableName + "' does not exist in the database.", ex.getCause().getMessage());
        verify(mockLogger).error(startsWith("Error describeTable for " + tableName), isA(IllegalArgumentException.class));
        verify(mockConnection, times(1)).close();
        verify(mockTableNotFoundResultSet, times(1)).close();
    }
    
    @Test
    void testDescribeTable_sqlExceptionDuringMetadataFetch() throws SQLException {
        String tableName = "error_table";
        ResultSet mockTableExistsResultSet = mock(ResultSet.class);
        when(mockDatabaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"})).thenReturn(mockTableExistsResultSet);
        when(mockTableExistsResultSet.next()).thenReturn(true); // Table exists

        SQLException sqlEx = new SQLException("Error fetching columns");
        when(mockDatabaseMetaData.getColumns(null, null, tableName, null)).thenThrow(sqlEx);

        ToolExecutionException ex = assertThrows(ToolExecutionException.class, () -> {
            explorerService.describeTable(null, null, tableName);
        });
        
        assertEquals(sqlEx, ex.getCause());
        verify(mockLogger).error(eq("Error describeTable for " + tableName + " message:" + sqlEx.getMessage()), eq(sqlEx));
        verify(mockConnection, times(1)).close();
        verify(mockTableExistsResultSet, times(1)).close(); // This one was closed before exception
    }

    @Test
    void testDescribeTable_indexInfoWithNullColumnName() throws SQLException {
        String tableName = "table_with_funky_index";
        ResultSet mockTableExistsResultSet = mock(ResultSet.class);
        when(mockDatabaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"})).thenReturn(mockTableExistsResultSet);
        when(mockTableExistsResultSet.next()).thenReturn(true);

        // Mock columns, PK, FK as empty for simplicity, focusing on index
        when(mockDatabaseMetaData.getColumns(null, null, tableName, null)).thenReturn(mock(ResultSet.class));
        when(mockDatabaseMetaData.getPrimaryKeys(null, null, tableName)).thenReturn(mock(ResultSet.class));
        when(mockDatabaseMetaData.getImportedKeys(null, null, tableName)).thenReturn(mock(ResultSet.class));


        ResultSet mockIndexResultSet = mock(ResultSet.class);
        when(mockDatabaseMetaData.getIndexInfo(null, null, tableName, false, false)).thenReturn(mockIndexResultSet);
        when(mockIndexResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockIndexResultSet.getString("INDEX_NAME")).thenReturn("some_index_name");
        when(mockIndexResultSet.getString("COLUMN_NAME")).thenReturn(null); // Null column name
        // getBoolean("NON_UNIQUE") won't be called if columnName is null

        Map<String, Object> tableInfo = explorerService.describeTable(null, null, tableName);
        List<Map<String, Object>> indexes = (List<Map<String, Object>>) tableInfo.get("indexes");
        assertTrue(indexes.isEmpty()); // Should be filtered out
    }
}