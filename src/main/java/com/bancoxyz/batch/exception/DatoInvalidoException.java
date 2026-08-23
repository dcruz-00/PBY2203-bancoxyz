package com.bancoxyz.batch.exception;

/**
 * Se lanza cuando un registro leído del CSV no cumple las reglas de
 * consistencia de datos definidas para el proceso (saldo inválido,
 * tipo no reconocido, fecha nula, etc.).
 *
 * Es unchecked (extiende RuntimeException) para no ensuciar la firma de
 * process() de los ItemProcessor. La usamos como "marcador" para que el
 * Step sepa qué excepciones puede omitir (skip) en vez de abortar el Job.
 */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}