package com.bancoxyz.batch.readers;

import com.bancoxyz.batch.model.CuentaAnual;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;

public class CuentasAnualesItemReader {

    public static FlatFileItemReader<CuentaAnual> reader() {
        return new FlatFileItemReaderBuilder<CuentaAnual>()
                .name("cuentasAnualesItemReader")
                .resource(new ClassPathResource("data/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(fieldSet -> {
                    CuentaAnual cuenta = new CuentaAnual();
                    cuenta.setCuentaId(fieldSet.readLong("cuenta_id"));
                    cuenta.setFecha(LocalDate.parse(fieldSet.readString("fecha")));
                    cuenta.setTransaccion(fieldSet.readString("transaccion"));
                    cuenta.setMonto(fieldSet.readDouble("monto"));
                    cuenta.setDescripcion(fieldSet.readString("descripcion"));
                    return cuenta;
                })
                .build();
    }
}