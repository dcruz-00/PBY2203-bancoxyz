package com.bancoxyz.bffweb.model;

import java.time.LocalDate;

public record TransaccionWebDTO(
        Long id,
        LocalDate fecha,
        Double monto,
        String tipo
) {}