package com.bancoxyz.batch.processors;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.model.CuentaAnual;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class CuentasAnualesItemProcessor implements ItemProcessor<CuentaAnual, CuentaAnual> {

    @Override
    public CuentaAnual process(CuentaAnual item) {
        // a diferencia de Transacciones,
        // aqui no se rechaza monto negativo: retiros/compras son datos validos.
        if (item.getCuentaId() == null) {
            throw new DatoInvalidoException("CuentaAnual: cuentaId nulo");
        }
        if (item.getFecha() == null) {
            throw new DatoInvalidoException("CuentaAnual id=" + item.getCuentaId() + ": fecha nula");
        }
        if (item.getMonto() == null) {
            throw new DatoInvalidoException("CuentaAnual id=" + item.getCuentaId() + ": monto nulo");
        }
        if (item.getTransaccion() == null || item.getTransaccion().isBlank()) {
            throw new DatoInvalidoException(
                    "CuentaAnual id=" + item.getCuentaId() + ": tipo de transaccion vacio");
        }

        return item;
    }
}