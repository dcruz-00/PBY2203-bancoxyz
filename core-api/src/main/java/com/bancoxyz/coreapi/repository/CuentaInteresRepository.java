package com.bancoxyz.coreapi.repository;

import com.bancoxyz.coreapi.model.CuentaInteresDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.bancoxyz.coreapi.exception.CuentaNoEncontradaException;
import com.bancoxyz.coreapi.exception.SaldoInsuficienteException;

import java.util.List;

@Repository
public class CuentaInteresRepository {

    private final JdbcTemplate jdbcTemplate;

    public CuentaInteresRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String CAMPOS = "cuenta_id, nombre, saldo, edad, tipo, interes_generado, fecha_calculo";

    public List<CuentaInteresDTO> findAll() {
        String sql = "SELECT " + CAMPOS + " FROM intereses_calculados";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CuentaInteresDTO(
                rs.getLong("cuenta_id"),
                rs.getString("nombre"),
                rs.getDouble("saldo"),
                rs.getInt("edad"),
                rs.getString("tipo"),
                rs.getDouble("interes_generado"),
                rs.getDate("fecha_calculo").toLocalDate()));
    }

    public CuentaInteresDTO findByCuentaId(Long cuentaId) {
        String sql = "SELECT " + CAMPOS + " FROM intereses_calculados WHERE cuenta_id = ?";
        List<CuentaInteresDTO> resultado = jdbcTemplate.query(sql, (rs, rowNum) -> new CuentaInteresDTO(
                rs.getLong("cuenta_id"),
                rs.getString("nombre"),
                rs.getDouble("saldo"),
                rs.getInt("edad"),
                rs.getString("tipo"),
                rs.getDouble("interes_generado"),
                rs.getDate("fecha_calculo").toLocalDate()), cuentaId);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public CuentaInteresDTO retirar(Long cuentaId, Double monto) {
        CuentaInteresDTO cuenta = findByCuentaId(cuentaId);
        if (cuenta == null) {
            throw new com.bancoxyz.coreapi.exception.CuentaNoEncontradaException(
                    "No existe la cuenta " + cuentaId);
        }
        if (cuenta.saldo() < monto) {
            throw new com.bancoxyz.coreapi.exception.SaldoInsuficienteException(
                    "Saldo insuficiente en cuenta " + cuentaId);
        }

        String sql = "UPDATE intereses_calculados SET saldo = saldo - ? WHERE cuenta_id = ?";
        jdbcTemplate.update(sql, monto, cuentaId);

        return findByCuentaId(cuentaId);
    }
}