package com.bancoxyz.coreapi.controller;

import com.bancoxyz.coreapi.model.CuentaInteresDTO;
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
        return repository.findByCuentaId(cuentaId);
    }
}