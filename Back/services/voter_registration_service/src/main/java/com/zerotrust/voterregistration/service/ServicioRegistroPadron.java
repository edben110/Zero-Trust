package com.zerotrust.voterregistration.service;

import com.zerotrust.voterregistration.domain.PadronInicial;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.port.FuentePadron;
import com.zerotrust.voterregistration.port.PublicadorElegibles;
import com.zerotrust.voterregistration.port.RepositorioPadron;

import java.time.Clock;

public class ServicioRegistroPadron {

    private final FuentePadron fuente;
    private final RepositorioPadron repositorio;
    private final PublicadorElegibles publicador;
    private final Clock reloj;

    public ServicioRegistroPadron(FuentePadron fuente, RepositorioPadron repositorio, PublicadorElegibles publicador,
                                  Clock reloj) {
        this.fuente = fuente;
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    // Primero la base de datos y después Kafka: si Kafka falla, repetir la tarea no duplica filas.
    public ResultadoRegistro registrar() {
        PadronInicial padron = PadronInicial.desde(fuente.leer(), reloj.instant());
        int nuevos = repositorio.guardarNuevos(padron.votantes());
        for (VotanteRegistrado votante : padron.votantes()) {
            publicador.publicar(votante);
        }
        return new ResultadoRegistro(padron.votantes().size(), nuevos);
    }
}
