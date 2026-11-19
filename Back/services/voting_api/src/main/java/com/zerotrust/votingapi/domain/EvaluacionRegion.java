package com.zerotrust.votingapi.domain;

/**
 * Resultado de evaluar la región y número de fallos de región que queda tras la evaluación.
 */
public record EvaluacionRegion(ResultadoRegion resultado, int fallos) {

    public EvaluacionRegion {
        if (resultado == null) {
            throw new IllegalArgumentException("resultado no puede ser nulo");
        }
        if (fallos < 0) {
            throw new IllegalArgumentException("fallos no puede ser negativo");
        }
    }

    public boolean fallosCambiaron(int fallosPrevios) {
        return fallos != fallosPrevios;
    }
}
