package com.zerotrust.votingapi.domain;

import com.zerotrust.votingapi.exception.VotoDuplicadoException;

public final class VotanteEnJornada {

    public static final int MAX_FALLOS_REGION = 2;

    private final Region regionRegistrada;
    private final boolean yaVoto;
    private final int fallosRegion;

    public VotanteEnJornada(Region regionRegistrada, boolean yaVoto, int fallosRegion) {
        if (regionRegistrada == null) {
            throw new IllegalArgumentException("regionRegistrada no puede ser nula");
        }
        if (fallosRegion < 0) {
            throw new IllegalArgumentException("fallosRegion no puede ser negativo");
        }
        this.regionRegistrada = regionRegistrada;
        this.yaVoto = yaVoto;
        this.fallosRegion = fallosRegion;
    }

    // El orden importa: primero el duplicado, luego la anulación previa y al final la región.
    public EvaluacionRegion evaluarRegion(Region seleccionada) {
        if (seleccionada == null) {
            throw new IllegalArgumentException("la región seleccionada no puede ser nula");
        }
        if (yaVoto) {
            throw new VotoDuplicadoException();
        }
        if (fallosRegion >= MAX_FALLOS_REGION) {
            return new EvaluacionRegion(ResultadoRegion.ANULADO, fallosRegion);
        }
        if (regionRegistrada.equals(seleccionada)) {
            return new EvaluacionRegion(ResultadoRegion.CORRECTA, fallosRegion);
        }
        int fallos = Math.min(MAX_FALLOS_REGION, fallosRegion + 1);
        ResultadoRegion resultado = fallos >= MAX_FALLOS_REGION ? ResultadoRegion.ANULADO : ResultadoRegion.PRIMER_ERROR;
        return new EvaluacionRegion(resultado, fallos);
    }
}
