package com.bancoxyz.batch.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

/**
 * Escucha los "skips" (omisiones) de un Step y los deja en el log.
 * Un solo listener genérico sirve para los 3 Jobs porque tipamos con
 * Object: Java permite usar SkipListener<Object,Object> en cualquier
 * Step gracias a los wildcards "? super I, ? super O" del builder.
 */
public class RegistroDescartadoListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(RegistroDescartadoListener.class);

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("Registro descartado en processor -> {} | motivo: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("Registro descartado en lectura | motivo: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.error("Registro descartado en escritura -> {} | motivo: {}", item, t.getMessage());
    }
}