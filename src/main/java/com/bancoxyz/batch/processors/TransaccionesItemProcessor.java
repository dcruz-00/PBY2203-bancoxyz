package com.bancoxyz.batch.processors;

import com.bancoxyz.batch.model.Transaccion;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.util.HashSet;
import java.util.Set;

public class TransaccionesItemProcessor implements ItemProcessor<Transaccion, Transaccion> {

    private final Set<String> vistos = new HashSet<>();

    @Override
    public Transaccion process(Transaccion item) {
        if (item.getMonto() == null || item.getMonto() <= 0) {
            return null;
        }

        String firma = item.getFecha() + "|" + item.getMonto() + "|" + item.getTipo();
        if (vistos.contains(firma)) {
            return null;
        }
        vistos.add(firma);

        return item;
    }
}