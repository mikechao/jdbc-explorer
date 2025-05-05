package com.mike.chao.jdbc.explorer.config;

import java.util.List;

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

        if (dbUrl.startsWith("jdbc:sqlite:")) {
            ds.setDriverClassName("org.sqlite.JDBC");
            // SQLite usually doesn't need username/password
        } else if (dbUrl.startsWith("jdbc:postgresql:")) {
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUsername(dbUsername);
            ds.setPassword(dbPassword);
        } else {
            throw new IllegalArgumentException("Unsupported DB URL: " + dbUrl);
        }
        return ds;
    }

}
