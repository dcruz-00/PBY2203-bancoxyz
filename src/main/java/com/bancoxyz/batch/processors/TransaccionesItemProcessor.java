package com.bancoxyz.batch.processors;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.model.Transaccion;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.util.HashSet;
import java.util.Set;

public class TransaccionesItemProcessor implements ItemProcessor<Transaccion, Transaccion> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("debito", "credito");

    private final Set<String> vistos = new HashSet<>();

    @Override
    public Transaccion process(Transaccion item) {

        if (item.getFecha() == null) {
            throw new DatoInvalidoException("Transaccion id=" + item.getId() + ": fecha nula");
        }
        if (item.getMonto() == null || item.getMonto() <= 0) {
            throw new DatoInvalidoException(
                    "Transaccion id=" + item.getId() + ": monto invalido (" + item.getMonto() + ")");
        }
        if (item.getTipo() == null || !TIPOS_VALIDOS.contains(item.getTipo().trim().toLowerCase())) {
            throw new DatoInvalidoException(
                    "Transaccion id=" + item.getId() + ": tipo invalido (" + item.getTipo() + ")");
        }

        String firma = item.getFecha() + "|" + item.getMonto() + "|" + item.getTipo();
        if (vistos.contains(firma)) {
            return null;
        }
        vistos.add(firma);

        return item;
    }
}