package com.inspien.eai.config;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.InputStream;

public class MyBatisConfig {

    private static SqlSessionFactory sqlSessionFactory;

    public static SqlSessionFactory getFactory() {

        if (sqlSessionFactory == null) {
            try {
                // Oracle DataSource
                DataSource dataSource = new PooledDataSource(
                        "oracle.jdbc.OracleDriver",
                        "jdbc:oracle:thin:@211.106.171.36:11527:POS",
                        "APPLICANT",
                        "inspien00"
                );

                // mybatis-config.xml 로드
                InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
                sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

                // 환경(DataSource/Tx) 주입
                TransactionFactory transactionFactory = new JdbcTransactionFactory();
                Environment environment = new Environment("default", transactionFactory, dataSource);
                sqlSessionFactory.getConfiguration().setEnvironment(environment);

            } catch (Exception e) {
                throw new RuntimeException("MyBatis 초기화 실패", e);
            }
        }

        return sqlSessionFactory;
    }
}
