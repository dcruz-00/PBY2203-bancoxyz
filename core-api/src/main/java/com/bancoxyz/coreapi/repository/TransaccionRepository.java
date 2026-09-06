package com.bancoxyz.coreapi.repository;

import com.bancoxyz.coreapi.model.TransaccionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransaccionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransaccionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TransaccionDTO> findAll() {
        String sql = "SELECT id, fecha, monto, tipo FROM transacciones_diarias ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TransaccionDTO(
                rs.getLong("id"),
                rs.getDate("fecha").toLocalDate(),
                rs.getDouble("monto"),
                rs.getString("tipo")
        ));
    }
}