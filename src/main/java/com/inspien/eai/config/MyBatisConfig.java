package com.inspien.eai.config;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class MyBatisConfig {

    private static SqlSessionFactory sqlSessionFactory;

    public static SqlSessionFactory getSqlSessionFactory() {

        if (sqlSessionFactory == null) {
            DataSource dataSource = new PooledDataSource(
                    "oracle.jdbc.driver.OracleDriver",
                    "jdbc:oracle:thin:@211.106.171.36:11527:POS",
                    "APPLICANT",
                    "inspien00"
            );

            sqlSessionFactory = new SqlSessionFactoryBuilder()
                    .build(
                            MyBatisConfig.class
                                    .getResourceAsStream("/mybatis-config.xml")
                    );

            sqlSessionFactory.getConfiguration().setEnvironment(
                    new org.apache.ibatis.mapping.Environment(
                            "default",
                            new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
                            dataSource
                    )
            );
        }
        return sqlSessionFactory;
    }
}
