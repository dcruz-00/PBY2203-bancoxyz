package com.bancoxyz.bffweb.controller;

import com.bancoxyz.bffweb.model.CuentaWebDTO;
import com.bancoxyz.bffweb.model.TransaccionWebDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/web")
public class CuentaWebController {

    private final RestClient coreApiClient;

    public CuentaWebController(RestClient coreApiClient) {
        this.coreApiClient = coreApiClient;
    }

    @GetMapping("/cuentas")
    public List<CuentaWebDTO> listarCuentas() {
        return coreApiClient.get()
                .uri("/api/cuentas")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CuentaWebDTO>>() {});
    }

    @GetMapping("/cuentas/{cuentaId}")
    public CuentaWebDTO obtenerCuenta(@PathVariable Long cuentaId) {
        return coreApiClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .body(CuentaWebDTO.class);
    }

    @GetMapping("/transacciones")
    public List<TransaccionWebDTO> listarTransacciones() {
        return coreApiClient.get()
                .uri("/api/transacciones")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionWebDTO>>() {});
    }
}