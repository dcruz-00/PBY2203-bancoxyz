package com.bancoxyz.coreapi.model;

import java.time.LocalDate;

public record CuentaInteresDTO(
        Long cuentaId,
        String nombre,
        Double saldo,
        Integer edad,
        String tipo,
        Double interesGenerado,
        LocalDate fechaCalculo
) {}