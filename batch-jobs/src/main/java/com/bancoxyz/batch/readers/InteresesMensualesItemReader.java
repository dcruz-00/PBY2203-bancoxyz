package com.bancoxyz.batch.readers;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;

import com.bancoxyz.batch.model.CuentaInteres;

public class InteresesMensualesItemReader {

    public static FlatFileItemReader<CuentaInteres> reader() {
        return new FlatFileItemReaderBuilder<CuentaInteres>()
                .name("interesesMensualesItemReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .delimited()
                .delimiter(",")
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .linesToSkip(1)
                .targetType(CuentaInteres.class)
                .build();
    }
}