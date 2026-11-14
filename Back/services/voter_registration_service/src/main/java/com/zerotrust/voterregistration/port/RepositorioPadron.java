package com.zerotrust.voterregistration.port;

import com.zerotrust.voterregistration.domain.VotanteRegistrado;

import java.util.List;

public interface RepositorioPadron {

    // Inserta solo los votantes que no existían y devuelve cuántos insertó.
    int guardarNuevos(List<VotanteRegistrado> votantes);
}
