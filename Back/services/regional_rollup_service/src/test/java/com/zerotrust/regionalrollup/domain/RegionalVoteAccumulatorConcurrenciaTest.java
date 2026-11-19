package com.zerotrust.regionalrollup.domain;

import com.zerotrust.regionalrollup.domain.strategy.SimpleCountStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionalVoteAccumulatorConcurrenciaTest {

    private static final int HILOS = 8;
    private static final int VOTOS_POR_HILO = 5_000;

    private static RawVoteEvent voto(String voterId, String region) {
        return new RawVoteEvent(UUID.randomUUID().toString(), uuid(voterId), uuid("c1"), region, "10.0.0.1", Instant.now());
    }

    private static String uuid(String nombre) {
        return UUID.nameUUIDFromBytes(nombre.getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Test
    @DisplayName("Debe devolver una copia de las regiones activas que no cambia con votos posteriores")
    void regionesActivasEsUnaCopia() {
        // Arrange
        RegionalVoteAccumulator acumulador = new RegionalVoteAccumulator(new SimpleCountStrategy());
        acumulador.accept(voto("v1", "norte"));

        // Act
        Set<String> regiones = acumulador.activeRegions();
        acumulador.accept(voto("v2", "sur"));

        // Assert
        assertEquals(Set.of("norte"), regiones);
    }

    @Test
    @DisplayName("Debe contar todos los votos y proyectar regiones sin errores mientras varios hilos aceptan votos")
    void aceptaYProyectaEnParalelo() throws Exception {
        // Arrange
        RegionalVoteAccumulator acumulador = new RegionalVoteAccumulator(new SimpleCountStrategy());
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
                    acumulador.accept(voto("h" + hilo + "-" + i, "region-" + (i % 200)));
                }
                return null;
            }));
        }
        Future<?> lector = pool.submit(() -> {
            salida.await();
            while (!terminado.get()) {
                for (String region : acumulador.activeRegions()) {
                    acumulador.snapshotFor(region);
                }
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
        long total = acumulador.activeRegions().stream()
                .flatMap(region -> acumulador.snapshotFor(region).results().stream())
                .mapToLong(RegionalCandidateResult::voteCount)
                .sum();
        assertEquals(HILOS * VOTOS_POR_HILO, total);
    }
}
