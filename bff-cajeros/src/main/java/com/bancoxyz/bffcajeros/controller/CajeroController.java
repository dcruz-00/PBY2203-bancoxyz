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
    public ResponseEntity<?> retirar(@PathVariable Long cuentaId, @RequestBody RetiroCajeroRequest request) {
        return coreApiClient.patch()
                .uri("/api/cuentas/{id}/retiro", cuentaId)
                .body(new RetiroCajeroRequest(request.monto()))
                .exchange((req, res) -> {
                    if (res.getStatusCode().isError()) {
                        String mensaje = new String(res.getBody().readAllBytes());
                        return ResponseEntity.status(res.getStatusCode()).body(mensaje);
                    }
                    CuentaCajeroDTO cuenta = res.bodyTo(CuentaCajeroDTO.class);
                    return ResponseEntity.ok(cuenta);
                });
    }
}