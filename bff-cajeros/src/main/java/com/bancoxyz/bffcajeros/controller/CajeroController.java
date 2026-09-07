package com.bancoxyz.bffcajeros.controller;

import com.bancoxyz.bffcajeros.model.CuentaCajeroDTO;
import com.bancoxyz.bffcajeros.model.RetiroCajeroRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/cajero")
public class CajeroController {

    private final RestClient coreApiClient;

    public CajeroController(RestClient coreApiClient) {
        this.coreApiClient = coreApiClient;
    }

    @GetMapping("/cuentas/{cuentaId}/saldo")
    public CuentaCajeroDTO consultarSaldo(@PathVariable Long cuentaId) {
        return coreApiClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .body(CuentaCajeroDTO.class);
    }

    @PatchMapping("/cuentas/{cuentaId}/retiro")
    public ResponseEntity<String> retirar(@PathVariable Long cuentaId, @RequestBody RetiroCajeroRequest request) {
        return coreApiClient.patch()
                .uri("/api/cuentas/{id}/retiro", cuentaId)
                .body(new com.bancoxyz.bffcajeros.model.RetiroCajeroRequest(request.monto()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {})
                .toEntity(String.class);
    }
}