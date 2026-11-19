package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.exception.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorElegibles;
import com.zerotrust.votingapi.port.RepositorioPadron;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Alta de votantes en el padrón desde el panel de administración.
// La cédula se convierte en el UUID de la elección en curso y no se guarda ni se publica.
public class ServicioPadron {

    private final DerivadorVoterId derivador;
    private final RepositorioPadron repositorio;
    private final PublicadorElegibles publicador;
    private final EjecutorTransaccional transaccion;
    private final Supplier<Catalogo> catalogo;
    private final ElectionId eleccion;
    private final Clock reloj;

    public ServicioPadron(DerivadorVoterId derivador, RepositorioPadron repositorio, PublicadorElegibles publicador,
                          EjecutorTransaccional transaccion, Supplier<Catalogo> catalogo, ElectionId eleccion,
                          Clock reloj) {
        this.derivador = derivador;
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.transaccion = transaccion;
        this.catalogo = catalogo;
        this.eleccion = eleccion;
        this.reloj = reloj;
    }

    // Inscripción y publicación forman una sola unidad: si Kafka no confirma, la fila se revierte
    // y el administrador puede reintentar. Si el documento ya estaba, lanza VotanteYaInscritoException.
    public void inscribir(String cedula, String region) {
        List<String> invalidos = new ArrayList<>();
        Cedula documento = convertir(() -> new Cedula(cedula), "cedula", invalidos);
        Region laRegion = convertir(() -> new Region(region), "region", invalidos);
        if (laRegion != null && !catalogo.get().existeRegion(laRegion)) {
            invalidos.add("region");
        }
        if (!invalidos.isEmpty()) {
            throw new SolicitudVotoInvalidaException(invalidos);
        }
        VotanteInscrito votante = new VotanteInscrito(derivador.derivar(documento, eleccion), laRegion,
                reloj.instant());
        transaccion.ejecutar(() -> {
            repositorio.inscribir(votante);
            publicador.publicar(votante);
            return null;
        });
    }

    private static <T> T convertir(Supplier<T> conversion, String campo, List<String> invalidos) {
        try {
            return conversion.get();
        } catch (IllegalArgumentException e) {
            invalidos.add(campo);
            return null;
        }
    }
}
