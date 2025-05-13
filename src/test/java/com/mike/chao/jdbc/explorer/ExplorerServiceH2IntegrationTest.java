package com.mike.chao.jdbc.explorer;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.mike.chao.jdbc.explorer.data.TableInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // To allow @BeforeAll and @AfterAll to be non-static
class ExplorerServiceH2IntegrationTest {

    private static JdbcDataSource h2DataSource;
    private ExplorerService explorerService;

    @BeforeAll
    void setupDatabase() throws SQLException {
        h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"); // DATABASE_TO_UPPER=false to keep table/column names as defined
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        try (Connection conn = h2DataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Drop tables if they exist from a previous unclean run (though usually not needed for new H2 mem instance)
            stmt.execute("DROP TABLE IF EXISTS \"OrderItems\"");
            stmt.execute("DROP TABLE IF EXISTS \"Orders\"");
            stmt.execute("DROP TABLE IF EXISTS \"Users\"");

            // Create Users table
            stmt.execute("CREATE TABLE \"Users\" (" +
                         "\"UserID\" INT PRIMARY KEY, " +
                         "\"Username\" VARCHAR(100) NOT NULL, " +
                         "\"Email\" VARCHAR(100) UNIQUE, " +
                         "\"RegistrationDate\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "\"Points\" INT DEFAULT 0" +
                         ")");

            // Create Orders table
            stmt.execute("CREATE TABLE \"Orders\" (" +
                         "\"OrderID\" INT PRIMARY KEY, " +
                         "\"UserID\" INT, " +
                         "\"OrderDate\" TIMESTAMP NOT NULL, " +
                         "\"TotalAmount\" DECIMAL(10, 2), " +
                         "FOREIGN KEY (\"UserID\") REFERENCES \"Users\"(\"UserID\")" +
                         ")");
            
            stmt.execute("CREATE INDEX \"idx_order_date\" ON \"Orders\"(\"OrderDate\")");


            // Insert sample data
            stmt.execute("INSERT INTO \"Users\" (\"UserID\", \"Username\", \"Email\", \"RegistrationDate\", \"Points\") VALUES " +
                         "(1, 'AliceSmith', 'alice.smith@example.com', '" + Timestamp.valueOf(LocalDateTime.now().minusDays(10)) + "', 150), " +
                         "(2, 'BobJohnson', 'bob.j@example.com', '" + Timestamp.valueOf(LocalDateTime.now().minusDays(5)) + "', 75), " +
                         "(3, 'CharlieBrown', null, '" + Timestamp.valueOf(LocalDateTime.now().minusDays(1)) + "', 0)");

            stmt.execute("INSERT INTO \"Orders\" (\"OrderID\", \"UserID\", \"OrderDate\", \"TotalAmount\") VALUES " +
                         "(101, 1, '" + Timestamp.valueOf(LocalDateTime.now().minusDays(2)) + "', 120.50), " +
                         "(102, 2, '" + Timestamp.valueOf(LocalDateTime.now().minusDays(1)) + "', 75.00), " +
                         "(103, 1, '" + Timestamp.valueOf(LocalDateTime.now().minusHours(5)) + "', 30.25)");
        }
        explorerService = new ExplorerService(h2DataSource);
    }

    @AfterAll
    void tearDownDatabase() throws SQLException {
        try (Connection conn = h2DataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"Orders\"");
            stmt.execute("DROP TABLE IF EXISTS \"Users\"");
            // Optionally, shutdown the H2 database if not using DB_CLOSE_DELAY=-1 or if specific cleanup is needed
            // stmt.execute("SHUTDOWN");
        }
    }


    @Test
    void testExecuteQuery_success() {
        List<Map<String, Object>> results = explorerService.executeQuery("SELECT \"UserID\", \"Username\", \"Email\" FROM \"Users\" WHERE \"Username\" = 'AliceSmith'");
        assertNotNull(results);
        assertEquals(1, results.size());
        Map<String, Object> alice = results.get(0);
        assertEquals(1, alice.get("UserID"));
        assertEquals("AliceSmith", alice.get("Username"));
        assertEquals("alice.smith@example.com", alice.get("Email"));

        List<Map<String, Object>> allUsers = explorerService.executeQuery("SELECT * FROM \"Users\" ORDER BY \"UserID\"");
        assertNotNull(allUsers);
        assertEquals(3, allUsers.size());
        assertNull(allUsers.get(2).get("Email")); // CharlieBrown has null email
        assertEquals(0, allUsers.get(2).get("Points")); // CharlieBrown has 0 points (default)
    }

    @Test
    void testGetTableNames_success() {
        List<TableInfo> tables = explorerService.getTableNames();
        assertNotNull(tables);
        assertTrue(tables.size() >= 2); // Expecting Users and Orders, H2 might have system tables too if not filtered strictly

        Optional<TableInfo> usersTableOpt = tables.stream()
            .filter(t -> "Users".equals(t.tableName()))
            .findFirst();
        assertTrue(usersTableOpt.isPresent(), "Users table not found");
        TableInfo usersTable = usersTableOpt.get();
        assertEquals("BASE TABLE", usersTable.tableType());
        assertEquals("PUBLIC", usersTable.schema()); // Default H2 schema

        Optional<TableInfo> ordersTableOpt = tables.stream()
            .filter(t -> "Orders".equals(t.tableName()))
            .findFirst();
        assertTrue(ordersTableOpt.isPresent(), "Orders table not found");
    }

    @Test
    void testDescribeTable_success_usersTable() {
        // H2 default catalog is the database name, often "TESTDB" for jdbc:h2:mem:testdb
        // Or null can be used for catalog if not specific. Schema is typically "PUBLIC".
        Map<String, Object> tableInfo = explorerService.describeTable(null, "PUBLIC", "Users");
        assertNotNull(tableInfo);
        assertEquals("Users", tableInfo.get("table"));

        // Columns
        List<Map<String, Object>> columns = (List<Map<String, Object>>) tableInfo.get("columns");
        assertNotNull(columns);
        assertTrue(columns.size() >= 5); // UserID, Username, Email, RegistrationDate, Points

        Map<String, Object> userIdCol = columns.stream().filter(c -> "UserID".equals(c.get("name"))).findFirst().orElse(null);
        assertNotNull(userIdCol);
        assertEquals("INTEGER", userIdCol.get("type")); // H2 type for INT
        assertFalse((Boolean) userIdCol.get("nullable"));

        Map<String, Object> usernameCol = columns.stream().filter(c -> "Username".equals(c.get("name"))).findFirst().orElse(null);
        assertNotNull(usernameCol);
        assertEquals("CHARACTER VARYING", usernameCol.get("type"));
        assertEquals(100, usernameCol.get("size"));
        assertFalse((Boolean) usernameCol.get("nullable")); // NOT NULL

        Map<String, Object> emailCol = columns.stream().filter(c -> "Email".equals(c.get("name"))).findFirst().orElse(null);
        assertNotNull(emailCol);
        assertEquals("CHARACTER VARYING", emailCol.get("type"));
        assertTrue((Boolean) emailCol.get("nullable")); // Nullable

        Map<String, Object> pointsCol = columns.stream().filter(c -> "Points".equals(c.get("name"))).findFirst().orElse(null);
        assertNotNull(pointsCol);
        assertEquals("INTEGER", pointsCol.get("type")); // H2 type for INT
        // Default values don't make a column non-nullable unless specified
        // H2's DatabaseMetaData for getColumns might report nullable true for columns with defaults if not explicitly NOT NULL
        // For "Points INT DEFAULT 0", nullable is true unless "Points INT NOT NULL DEFAULT 0"

        // Primary Keys
        List<String> primaryKeys = (List<String>) tableInfo.get("primaryKeys");
        assertNotNull(primaryKeys);
        assertEquals(1, primaryKeys.size());
        assertEquals("UserID", primaryKeys.get(0));

        // Foreign Keys (Users table has no outgoing FKs in this setup)
        List<Map<String, Object>> foreignKeys = (List<Map<String, Object>>) tableInfo.get("foreignKeys");
        assertNotNull(foreignKeys);
        assertTrue(foreignKeys.isEmpty());

        // Indexes (H2 creates an index for PRIMARY KEY and UNIQUE constraints automatically)
        List<Map<String, Object>> indexes = (List<Map<String, Object>>) tableInfo.get("indexes");
        assertNotNull(indexes);
        // Expecting at least PK index and unique index for Email
        assertTrue(indexes.stream().anyMatch(idx -> "UserID".equals(idx.get("column")) && (Boolean)idx.get("unique")));
        assertTrue(indexes.stream().anyMatch(idx -> "Email".equals(idx.get("column")) && (Boolean)idx.get("unique")));
    }

    @Test
    void testDescribeTable_success_ordersTable() {
        Map<String, Object> tableInfo = explorerService.describeTable(null, "PUBLIC", "Orders");
        assertNotNull(tableInfo);
        assertEquals("Orders", tableInfo.get("table"));

        // Columns
        List<Map<String, Object>> columns = (List<Map<String, Object>>) tableInfo.get("columns");
        Map<String, Object> orderIdCol = columns.stream().filter(c -> "OrderID".equals(c.get("name"))).findFirst().orElse(null);
        assertNotNull(orderIdCol);
        assertEquals("INTEGER", orderIdCol.get("type"));
        assertFalse((Boolean) orderIdCol.get("nullable"));

        Map<String, Object> userIdCol = columns.stream().filter(c -> "UserID".equals(c.get("name"))).findFirst().orElse(null);
        assertNotNull(userIdCol);
        assertEquals("INTEGER", userIdCol.get("type"));
        assertTrue((Boolean) userIdCol.get("nullable")); // FKs can be nullable unless specified NOT NULL

        // Primary Keys
        List<String> primaryKeys = (List<String>) tableInfo.get("primaryKeys");
        assertEquals(1, primaryKeys.size());
        assertEquals("OrderID", primaryKeys.get(0));

        // Foreign Keys
        List<Map<String, Object>> foreignKeys = (List<Map<String, Object>>) tableInfo.get("foreignKeys");
        assertNotNull(foreignKeys);
        assertEquals(1, foreignKeys.size());
        Map<String, Object> fk = foreignKeys.get(0);
        assertEquals("UserID", fk.get("column"));
        assertEquals("Users", fk.get("referencesTable"));
        assertEquals("UserID", fk.get("referencesColumn"));
        
        // Indexes
        List<Map<String, Object>> indexes = (List<Map<String, Object>>) tableInfo.get("indexes");
        assertNotNull(indexes);
        // Expecting at least PK index and our custom idx_order_date
        assertTrue(indexes.stream().anyMatch(idx -> "OrderID".equals(idx.get("column")) && (Boolean)idx.get("unique")));
        Optional<Map<String,Object>> orderDateIndex = indexes.stream().filter(idx -> "idx_order_date".equalsIgnoreCase((String)idx.get("name"))).findFirst();
        assertTrue(orderDateIndex.isPresent());
        assertEquals("OrderDate", orderDateIndex.get().get("column"));
        assertFalse((Boolean)orderDateIndex.get().get("unique")); // Our index is not unique
    }
}