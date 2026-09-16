package com.bancoxyz.coreapi.controller;

import com.bancoxyz.coreapi.exception.CuentaNoEncontradaException;
import com.bancoxyz.coreapi.model.CuentaInteresDTO;
import com.bancoxyz.coreapi.model.RetiroRequest;
import com.bancoxyz.coreapi.repository.CuentaInteresRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaInteresRepository repository;

    public CuentaController(CuentaInteresRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CuentaInteresDTO> listar() {
        return repository.findAll();
    }

    @GetMapping("/{cuentaId}")
    public CuentaInteresDTO obtener(@PathVariable Long cuentaId) {
        CuentaInteresDTO cuenta = repository.findByCuentaId(cuentaId);
        if (cuenta == null) {
            throw new CuentaNoEncontradaException("No existe la cuenta " + cuentaId);
        }
        return cuenta;
    }

    @PatchMapping("/{cuentaId}/retiro")
    public CuentaInteresDTO retirar(@PathVariable Long cuentaId, @RequestBody RetiroRequest request) {
        return repository.retirar(cuentaId, request.monto());
    }
}