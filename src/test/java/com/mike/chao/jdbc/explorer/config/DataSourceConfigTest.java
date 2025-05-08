package com.mike.chao.jdbc.explorer.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest(classes = DataSourceConfig.class)
@TestPropertySource(properties = {
    "db.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "db.username=sa",
    "db.password="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DataSourceConfigTest {

    @Autowired(required = false)
    private DataSource dataSource;

    @Test
    void testH2DataSourceCreated() {
        assertNotNull(dataSource);
        assertEquals("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", ((org.springframework.jdbc.datasource.DriverManagerDataSource) dataSource).getUrl());
        assertEquals("sa", ((org.springframework.jdbc.datasource.DriverManagerDataSource) dataSource).getUsername());
    }

    @Test
    void testUnsupportedDbUrlThrowsException() {
        new ApplicationContextRunner()
            .withUserConfiguration(DataSourceConfig.class)
            .withPropertyValues(
                "db.url=jdbc:unsupported://localhost:1234/db",
                "db.username=test",
                "db.password=test"
            )
            .run(context -> assertThatThrownBy(() -> context.getBean(javax.sql.DataSource.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .rootCause()
                .hasMessageContaining("Unsupported DB URL"));
    }

}