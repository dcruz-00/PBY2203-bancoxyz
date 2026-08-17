package com.bancoxyz.batch.model;

import java.time.LocalDate;

public class CuentaInteres {
    private Long cuentaId;
    private String nombre;
    private Double saldo;
    private Integer edad;
    private String tipo;
    private Double tasaAplicada;    
    private Double interesGenerado;
    private LocalDate fechaCalculo;

    public CuentaInteres() {}
    public Long getCuentaId() {return cuentaId;}
    public void setCuentaId(Long cuentaId) {this.cuentaId = cuentaId;}
    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}
    public Double getSaldo() {return saldo;}
    public void setSaldo(Double saldo) {this.saldo = saldo;}
    public Integer getEdad() {return edad;}
    public void setEdad(Integer edad) {this.edad = edad;}
    public String getTipo() {return tipo;}
    public void setTipo(String tipo) {this.tipo = tipo;}
    public Double getTasaAplicada() {return tasaAplicada;}
    public void setTasaAplicada(Double tasaAplicada) {this.tasaAplicada = tasaAplicada;}
    public Double getInteresGenerado() {return interesGenerado;}
    public void setInteresGenerado(Double interesGenerado) {this.interesGenerado = interesGenerado;}
    public LocalDate getFechaCalculo() {return fechaCalculo;}
    public void setFechaCalculo(LocalDate fechaCalculo) {this.fechaCalculo = fechaCalculo;}

    @Override
    public String toString() {
        return "CuentaInteres{" + "cuentaId=" + cuentaId + ", nombre='" + nombre + '\'' +
                ", saldo=" + saldo + ", tipo='" + tipo + '\'' + ", interesGenerado=" + interesGenerado +
                ", fechaCalculo=" + fechaCalculo + '}';
    }
}