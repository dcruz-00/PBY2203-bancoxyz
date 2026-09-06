package com.bancoxyz.bffweb.model;

import java.time.LocalDate;

public record CuentaWebDTO(
        Long cuentaId,
        String nombre,
        Double saldo,
        Integer edad,
        String tipo,
        Double interesGenerado,
        LocalDate fechaCalculo
) {}