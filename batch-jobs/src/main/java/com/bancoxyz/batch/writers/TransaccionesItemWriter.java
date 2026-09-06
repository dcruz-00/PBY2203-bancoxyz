package com.bancoxyz.batch.writers;

import com.bancoxyz.batch.model.Transaccion;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;

public class TransaccionesItemWriter {

    public static JdbcBatchItemWriter<Transaccion> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaccion>()
                .dataSource(dataSource)
                .sql("INSERT INTO transacciones_diarias (id, fecha, monto, tipo) VALUES (:id, :fecha, :monto, :tipo)")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }
}