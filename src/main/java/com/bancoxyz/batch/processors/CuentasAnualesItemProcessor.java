package com.bancoxyz.batch.processors;

import com.bancoxyz.batch.model.CuentaAnual;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class CuentasAnualesItemProcessor implements ItemProcessor<CuentaAnual, CuentaAnual> {

    @Override
    public CuentaAnual process(CuentaAnual item) {
        return item;
    }
}