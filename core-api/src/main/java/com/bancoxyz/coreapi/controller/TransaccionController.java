package com.bancoxyz.coreapi.controller;

import com.bancoxyz.coreapi.model.TransaccionDTO;
import com.bancoxyz.coreapi.repository.TransaccionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionRepository repository;

    public TransaccionController(TransaccionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TransaccionDTO> listar() {
        return repository.findAll();
    }
}