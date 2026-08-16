package com.bancoxyz.batch.model;

import java.time.LocalDate;

public class CuentaAnual {
    private Long cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private Double monto;
    private String descripcion;

    public Long getCuentaId() { return cuentaId; }
    public void setCuentaId(Long cuentaId) { this.cuentaId = cuentaId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getTransaccion() { return transaccion; }
    public void setTransaccion(String transaccion) { this.transaccion = transaccion; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return "CuentaAnual{cuentaId=" + cuentaId + ", fecha=" + fecha +
               ", transaccion='" + transaccion + "', monto=" + monto +
               ", descripcion='" + descripcion + "'}";
    }
}