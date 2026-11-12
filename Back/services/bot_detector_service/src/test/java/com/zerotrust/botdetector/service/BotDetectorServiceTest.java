package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.FraudIncidentRepository;
import com.zerotrust.botdetector.domain.port.SecurityAlertPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prueba de orquestación. Verifica que cuando el estado de la IP genera alerta,
 * el servicio llama exactamente una vez al publisher Y al repositorio.
 */
@ExtendWith(MockitoExtension.class)
class BotDetectorServiceTest {

    @Mock
    private SecurityAlertPublisher  alertPublisher;

    @Mock
    private FraudIncidentRepository repository;

    // Umbrales pequeños para provocar alertas rápido en los tests
    @InjectMocks
    private BotDetectorService service = new BotDetectorService(
            null, null, 2, 4, 60);

    // Re-creamos el servicio con mocks inyectados manualmente
    // porque @InjectMocks no soporta parámetros @Value
    private BotDetectorService buildService() {
        return new BotDetectorService(alertPublisher, repository, 2, 4, 60);
    }

    @Test
    void noPublicaAlertaBajoUmbral() {
        // Arrange
        BotDetectorService svc = buildService();

        // Act: 2 votos, umbral sospechosa = 2, no se supera aún
        svc.evaluate("1.1.1.1");
        svc.evaluate("1.1.1.1");

        // Assert
        verify(alertPublisher, never()).publish(any());
        verify(repository, never()).save(any());
    }

    @Test
    void publicaAlertaYPersisteCuandoSuperaUmbral() {
        // Arrange
        BotDetectorService svc = buildService();
        svc.evaluate("2.2.2.2");
        svc.evaluate("2.2.2.2");

        // Act: 3er voto supera umbral sospechosa (2)
        svc.evaluate("2.2.2.2");

        // Assert: alerta publicada Y persistida exactamente una vez
        verify(alertPublisher, times(1)).publish(any(SecurityAlert.class));
        verify(repository,     times(1)).save(any(SecurityAlert.class));
    }

    @Test
    void ipsDiferentesNoSeInterfirenEntreEllas() {
        // Arrange
        BotDetectorService svc = buildService();

        // Act: IP-A supera umbral, IP-B no
        svc.evaluate("3.3.3.3");
        svc.evaluate("3.3.3.3");
        svc.evaluate("3.3.3.3"); // supera umbral

        svc.evaluate("4.4.4.4"); // solo 1 voto

        // Assert: solo IP-A generó alerta
        verify(alertPublisher, times(1)).publish(any());
    }
}
