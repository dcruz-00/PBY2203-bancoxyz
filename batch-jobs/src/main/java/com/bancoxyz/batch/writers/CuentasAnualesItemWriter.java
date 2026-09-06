package com.bancoxyz.batch.writers;

import com.bancoxyz.batch.model.CuentaAnual;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;

public class CuentasAnualesItemWriter {

    public static JdbcBatchItemWriter<CuentaAnual> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaAnual>()
                .dataSource(dataSource)
                .sql("INSERT INTO cuentas_anuales (cuenta_id, fecha, transaccion, monto, descripcion) " +
                     "VALUES (:cuentaId, :fecha, :transaccion, :monto, :descripcion)")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }
}