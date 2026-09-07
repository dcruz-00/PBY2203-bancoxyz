package com.bancoxyz.bffmovil.controller;

import com.bancoxyz.bffmovil.model.CuentaMovilDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/movil")
public class CuentaMovilController {

    private final RestClient coreApiClient;

    public CuentaMovilController(RestClient coreApiClient) {
        this.coreApiClient = coreApiClient;
    }

    @GetMapping("/cuentas")
    public List<CuentaMovilDTO> listarCuentas() {
        return coreApiClient.get()
                .uri("/api/cuentas")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CuentaMovilDTO>>() {});
    }

    @GetMapping("/cuentas/{cuentaId}")
    public CuentaMovilDTO obtenerCuenta(@PathVariable Long cuentaId) {
        return coreApiClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .body(CuentaMovilDTO.class);
    }
}