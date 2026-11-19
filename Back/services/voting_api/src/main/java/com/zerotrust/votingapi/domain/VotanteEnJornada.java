package com.zerotrust.votingapi.domain;

import com.zerotrust.votingapi.exception.VotoDuplicadoException;

/**
 * Situación de un votante del padrón en la jornada activa: su región registrada,
 * si ya votó en esta elección y cuántas veces eligió una región distinta.
 */
public final class VotanteEnJornada {

    /** Al llegar a este número de regiones incorrectas el voto queda anulado. */
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

    /**
     * Aplica las reglas de voto único y región; el orden importa: primero el duplicado,
     * después la anulación previa y por último la comparación de región.
     */
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
