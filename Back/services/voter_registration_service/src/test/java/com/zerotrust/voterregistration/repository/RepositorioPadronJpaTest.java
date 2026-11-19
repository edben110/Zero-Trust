package com.zerotrust.voterregistration.repository;

import com.zerotrust.voterregistration.repository.entity.VotanteRegistradoEntity;
import com.zerotrust.voterregistration.domain.Region;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RepositorioPadronJpa")
@ExtendWith(MockitoExtension.class)
class RepositorioPadronJpaTest {

    private static final String ID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String ID_2 = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d";
    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    private static VotanteRegistrado votante(String id) {
        return new VotanteRegistrado(new VoterId(id), new Region("norte"), AHORA);
    }

    @Mock
    private VotanteJpaRepository jpa;

    @Test
    @DisplayName("Solo debe insertar los votantes que no existían")
    void soloNuevos() {
        when(jpa.buscarVoterIdsExistentes(anyCollection())).thenReturn(Set.of(ID_1));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VotanteRegistradoEntity>> insertados = ArgumentCaptor.forClass(List.class);

        int nuevos = new RepositorioPadronJpa(jpa).guardarNuevos(List.of(votante(ID_1), votante(ID_2)));

        verify(jpa, times(1)).saveAll(insertados.capture());
        assertThat(insertados.getValue()).extracting(VotanteRegistradoEntity::getVoterId).containsExactly(ID_2);
        assertThat(nuevos).isEqualTo(1);
    }
}
