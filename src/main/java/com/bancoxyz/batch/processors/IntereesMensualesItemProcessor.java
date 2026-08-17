package com.bancoxyz.batch.processors;

import com.bancoxyz.batch.model.CuentaInteres;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.time.LocalDate;

public class IntereesMensualesItemProcessor implements ItemProcessor<CuentaInteres, CuentaInteres> {
    private static final double TASA_AHORRO = 0.005;   
    private static final double TASA_PRESTAMO = 0.015; 

    @Override
    public CuentaInteres process(CuentaInteres item) {
        double tasa;

        switch (item.getTipo().toLowerCase()) {
            case "ahorro":
                tasa = TASA_AHORRO;
                break;
            case "prestamo":
                tasa = TASA_PRESTAMO;
                break;
            default:
                return null;
        }

        double interes = item.getSaldo() * tasa;

        item.setTasaAplicada(tasa);
        item.setInteresGenerado(interes);
        item.setFechaCalculo(LocalDate.now());

        return item;
    }
}