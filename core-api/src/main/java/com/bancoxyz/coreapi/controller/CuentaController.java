package com.bancoxyz.coreapi.controller;

import com.bancoxyz.coreapi.exception.CuentaNoEncontradaException;
import com.bancoxyz.coreapi.exception.SaldoInsuficienteException;
import com.bancoxyz.coreapi.model.CuentaInteresDTO;
import com.bancoxyz.coreapi.model.RetiroRequest;
import com.bancoxyz.coreapi.repository.CuentaInteresRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        return repository.findByCuentaId(cuentaId);
    }

    @PatchMapping("/{cuentaId}/retiro")
    public ResponseEntity<?> retirar(@PathVariable Long cuentaId, @RequestBody RetiroRequest request) {
        try {
            CuentaInteresDTO actualizada = repository.retirar(cuentaId, request.monto());
            return ResponseEntity.ok(actualizada);
        } catch (CuentaNoEncontradaException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SaldoInsuficienteException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}