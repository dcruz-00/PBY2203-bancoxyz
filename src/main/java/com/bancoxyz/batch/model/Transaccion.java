package com.bancoxyz.batch.model;

import java.time.LocalDate;

public class Transaccion {
    private Long id;
    private LocalDate fecha;
    private Double monto;
    private String tipo;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return "Transaccion{id=" + id + ", fecha=" + fecha + ", monto=" + monto + ", tipo='" + tipo + "'}";
    }
}