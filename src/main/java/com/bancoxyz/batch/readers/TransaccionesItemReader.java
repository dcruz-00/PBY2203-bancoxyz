package com.bancoxyz.batch.readers;

import java.time.LocalDate;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.ClassPathResource;

import com.bancoxyz.batch.model.Transaccion;

public class TransaccionesItemReader {

    public static FlatFileItemReader<Transaccion> reader() {
        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionesItemReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(fieldSet -> {
                    Transaccion transaccion = new Transaccion();
                    transaccion.setId(fieldSet.readLong("id"));
                    transaccion.setFecha(LocalDate.parse(fieldSet.readString("fecha")));
                    transaccion.setMonto(fieldSet.readDouble("monto"));
                    transaccion.setTipo(fieldSet.readString("tipo"));
                    return transaccion;
                })
                .build();
    }

    public static FlatFileItemReader<Transaccion> reader(int fromItem, int toItem) {
        FlatFileItemReader<Transaccion> reader = reader();
        reader.setName("transaccionesItemReader-" + fromItem + "-" + toItem);
        reader.setCurrentItemCount(fromItem);
        reader.setMaxItemCount(toItem);
        return reader;
    }
}