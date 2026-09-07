package com.bancoxyz.bffmovil.model;

public record CuentaMovilDTO(
        Long cuentaId,
        Double saldo,
        String tipo
) {}