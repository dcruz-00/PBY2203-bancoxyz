package com.bancoxyz.batch.processors;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.model.CuentaInteres;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class IntereesMensualesItemProcessor implements ItemProcessor<CuentaInteres, CuentaInteres> {
    private static final double TASA_AHORRO = 0.005;
    private static final double TASA_PRESTAMO = 0.015;
    private static final int EDAD_MINIMA = 0;
    private static final int EDAD_MAXIMA = 120;

    // Guarda una "firma" de nombre+saldo+edad+tipo ya vistos, para detectar duplicados
    private final Set<String> vistos = new HashSet<>();

    @Override
    public CuentaInteres process(CuentaInteres item) {
        // Reglas de consistencia
        if (item.getNombre() == null || item.getNombre().isBlank()) {
            throw new DatoInvalidoException("Cuenta id=" + item.getCuentaId() + ": nombre vacio");
        }
        if (item.getSaldo() == null || item.getSaldo() < 0) {
            throw new DatoInvalidoException(
                    "Cuenta id=" + item.getCuentaId() + ": saldo invalido (" + item.getSaldo() + ")");
        }
        if (item.getEdad() == null || item.getEdad() < EDAD_MINIMA || item.getEdad() > EDAD_MAXIMA) {
            throw new DatoInvalidoException(
                    "Cuenta id=" + item.getCuentaId() + ": edad fuera de rango (" + item.getEdad() + ")");
        }

        double tasa;
        String tipo = item.getTipo() == null ? "" : item.getTipo().toLowerCase();
        switch (tipo) {
            case "ahorro":
                tasa = TASA_AHORRO;
                break;
            case "prestamo":
                tasa = TASA_PRESTAMO;
                break;
            default:
                throw new DatoInvalidoException(
                        "Cuenta id=" + item.getCuentaId() + ": tipo no soportado (" + item.getTipo() + ")");
        }

        // Regla: duplicado por nombre+saldo+edad+tipo (mismos datos, distinto cuenta_id) -> se omite
        String firma = item.getNombre() + "|" + item.getSaldo() + "|" + item.getEdad() + "|" + item.getTipo();
        if (vistos.contains(firma)) {
            return null;
        }
        vistos.add(firma);

        double interes = item.getSaldo() * tasa;

        item.setTasaAplicada(tasa);
        item.setInteresGenerado(interes);
        item.setFechaCalculo(LocalDate.now());

        return item;
    }
}