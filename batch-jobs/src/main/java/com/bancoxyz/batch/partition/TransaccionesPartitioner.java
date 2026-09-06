package com.bancoxyz.batch.partition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.Resource;

public class TransaccionesPartitioner implements Partitioner {

    private final Resource resource;

    public TransaccionesPartitioner(Resource resource) {
        this.resource = resource;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        int totalRegistros = contarRegistros();
        Map<String, ExecutionContext> particiones = new HashMap<>();

        if (totalRegistros == 0) {
            return particiones;
        }

        int tamanoBase = totalRegistros / gridSize;
        int resto = totalRegistros % gridSize;

        int fromItem = 0;
        for (int i = 0; i < gridSize; i++) {
            int tamanoParticion = tamanoBase + (i < resto ? 1 : 0);
            if (tamanoParticion == 0) {
                continue;
            }
            int toItem = fromItem + tamanoParticion;

            ExecutionContext contexto = new ExecutionContext();
            contexto.putInt("fromItem", fromItem);
            contexto.putInt("toItem", toItem);
            particiones.put("partition" + i, contexto);

            fromItem = toItem;
        }

        return particiones;
    }

    private int contarRegistros() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            long lineas = br.lines().count();
            return (int) Math.max(0, lineas - 1); // -1 por la fila de encabezado
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo contar registros de " + resource.getFilename(), e);
        }
    }
}