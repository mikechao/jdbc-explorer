package com.mike.chao.jdbc.explorer.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DataSourceConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.username:}")
    private String dbUsername;

    @Value("${db.password:}")
    private String dbPassword;

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(dbUrl);

        Map<String, String> driverClassByPrefix = Map.of(
            "jdbc:sqlite:", "org.sqlite.JDBC",
            "jdbc:postgresql:", "org.postgresql.Driver",
            "jdbc:h2:", "org.h2.Driver"
        );

        String driverClassName = null;
        for (String prefix : driverClassByPrefix.keySet()) {
            if (dbUrl.startsWith(prefix)) {
                driverClassName = driverClassByPrefix.get(prefix);
                ds.setDriverClassName(driverClassName);
                if (!"jdbc:sqlite:".equals(prefix)) {
                    ds.setUsername(dbUsername);
                    ds.setPassword(dbPassword);
                }
                break;
            }
        }

        if (driverClassName == null) {
            throw new IllegalArgumentException("Unsupported DB URL: " + dbUrl);
        }
        
        return ds;
    }

}
