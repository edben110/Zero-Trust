package com.zerotrust.voteprocessor.domain;

import com.zerotrust.voteprocessor.domain.model.CandidateId;
import com.zerotrust.voteprocessor.domain.model.CandidateResult;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.model.VoteId;
import com.zerotrust.voteprocessor.domain.model.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteAggregateConcurrenciaTest {

    private static final int HILOS = 8;
    private static final int VOTOS_POR_HILO = 5_000;

    private static RawVoteEvent voto(String voterId, String candidato) {
        return new RawVoteEvent(new VoteId(UUID.randomUUID().toString()), new VoterId(uuid(voterId)),
                new CandidateId(uuid(candidato)), "norte", "10.0.0.1", Instant.now());
    }

    private static String uuid(String nombre) {
        return UUID.nameUUIDFromBytes(nombre.getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Test
    @DisplayName("Debe contar todos los votos y tomar snapshots sin errores mientras varios hilos aceptan votos")
    void aceptaYProyectaEnParalelo() throws Exception {
        // Arrange
        VoteAggregate agregado = new VoteAggregate();
        ExecutorService pool = Executors.newFixedThreadPool(HILOS + 1);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicBoolean terminado = new AtomicBoolean(false);
        List<Future<?>> tareas = new ArrayList<>();

        // Act
        for (int h = 0; h < HILOS; h++) {
            int hilo = h;
            tareas.add(pool.submit(() -> {
                salida.await();
                for (int i = 0; i < VOTOS_POR_HILO; i++) {
                    agregado.accept(voto("h" + hilo + "-" + i, "c" + (i % 50)));
                }
                return null;
            }));
        }
        Future<?> lector = pool.submit(() -> {
            salida.await();
            while (!terminado.get()) {
                agregado.snapshot();
            }
            return null;
        });
        salida.countDown();
        for (Future<?> tarea : tareas) {
            tarea.get(30, TimeUnit.SECONDS);
        }
        terminado.set(true);
        lector.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        // Assert
        long sumaSnapshot = agregado.snapshot().results().stream().mapToLong(CandidateResult::voteCount).sum();
        assertEquals(HILOS * VOTOS_POR_HILO, agregado.totalVotes());
        assertEquals(HILOS * VOTOS_POR_HILO, sumaSnapshot);
    }

    @Test
    @DisplayName("Debe aceptar una sola vez al mismo votante aunque llegue desde varios hilos a la vez")
    void unVotoPorVotanteEnParalelo() throws Exception {
        // Arrange
        VoteAggregate agregado = new VoteAggregate();
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> tareas = new ArrayList<>();

        // Act
        for (int h = 0; h < HILOS; h++) {
            tareas.add(pool.submit(() -> {
                salida.await();
                int aceptados = 0;
                for (int i = 0; i < VOTOS_POR_HILO; i++) {
                    if (agregado.accept(voto("votante-" + i, "c1"))) {
                        aceptados++;
                    }
                }
                return aceptados;
            }));
        }
        salida.countDown();
        int totalAceptados = 0;
        for (Future<Integer> tarea : tareas) {
            totalAceptados += tarea.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // Assert
        assertEquals(VOTOS_POR_HILO, totalAceptados);
        assertEquals(VOTOS_POR_HILO, agregado.totalVotes());
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
    }
}
