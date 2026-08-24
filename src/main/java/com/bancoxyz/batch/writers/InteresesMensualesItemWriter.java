package com.bancoxyz.batch.writers;

import com.bancoxyz.batch.model.CuentaInteres;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;

public class IntereesMensualesItemWriter {

    public static JdbcBatchItemWriter<CuentaInteres> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaInteres>()
                .dataSource(dataSource)
                .sql("INSERT INTO intereses_calculados " +
                        "(cuenta_id, nombre, saldo, edad, tipo, interes_generado, fecha_calculo) " +
                        "VALUES (:cuentaId, :nombre, :saldo, :edad, :tipo, :interesGenerado, :fechaCalculo)")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .build();
    }
}