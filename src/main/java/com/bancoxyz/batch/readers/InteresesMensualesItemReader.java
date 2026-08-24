package com.bancoxyz.batch.readers;

import com.bancoxyz.batch.model.CuentaInteres;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;

public class IntereesMensualesItemReader {

    public static FlatFileItemReader<CuentaInteres> reader() {
        return new FlatFileItemReaderBuilder<CuentaInteres>()
                .name("intereesMensualesItemReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .delimited()
                .delimiter(",")
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .linesToSkip(1)
                .targetType(CuentaInteres.class)
                .build();
    }
}