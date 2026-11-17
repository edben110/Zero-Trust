package com.zerotrust.analyticsarchiver.service;

import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import com.zerotrust.analyticsarchiver.port.RepositorioAuditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioArchivoVotosTest {

    private static final VotoAuditado VOTO = new VotoAuditado(new VoteId("11111111-2222-4333-8444-555555555555"),
            new VoterId("3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b"), new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"),
            new Region("norte"), new SourceIp("203.0.113.7"), Instant.parse("2026-11-14T15:00:00Z"));

    @Mock
    private RepositorioAuditoria repositorio;

    @InjectMocks
    private ServicioArchivoVotos servicio;

    @Test
    @DisplayName("Debe persistir exactamente una vez cada voto nuevo")
    void persisteUnaVez() {
        when(repositorio.guardarSiNoExiste(VOTO)).thenReturn(true);

        boolean nuevo = servicio.archivar(VOTO);

        assertThat(nuevo).isTrue();
        verify(repositorio, times(1)).guardarSiNoExiste(VOTO);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    @DisplayName("Un voto repetido no se inserta otra vez y no es un error")
    void repetido() {
        when(repositorio.guardarSiNoExiste(VOTO)).thenReturn(false);

        assertThat(servicio.archivar(VOTO)).isFalse();
        verify(repositorio, times(1)).guardarSiNoExiste(VOTO);
    }

    @Test
    @DisplayName("Un fallo de la base de datos se propaga para no confirmar el offset")
    void falloBaseDeDatos() {
        when(repositorio.guardarSiNoExiste(VOTO)).thenThrow(new IllegalStateException("sin base de datos"));

        assertThrows(IllegalStateException.class, () -> servicio.archivar(VOTO));
    }
}
