package com.zerotrust.authserver.adapter.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.zerotrust.authserver.domain.ClientId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroEventosSlf4jTest {

    private final RegistroEventosSlf4j registro = new RegistroEventosSlf4j();
    private final ListAppender<ILoggingEvent> capturados = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RegistroEventosSlf4j.class);

    @BeforeEach
    void capturar() {
        capturados.start();
        logger.addAppender(capturados);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(capturados);
    }

    @Test
    @DisplayName("Una emisión registra una sola línea INFO con el client_id")
    void emision() {
        registro.emisionCorrecta(new ClientId("voting_api"));

        assertThat(capturados.list).hasSize(1);
        assertThat(capturados.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(capturados.list.get(0).getFormattedMessage()).contains("emitido").contains("voting_api");
    }

    @Test
    @DisplayName("Un rechazo de un cliente registrado registra una línea WARN con el código y el client_id")
    void rechazoRegistrado() {
        registro.rechazoClienteRegistrado("invalid_client", new ClientId("voting_api"));

        assertThat(capturados.list).hasSize(1);
        assertThat(capturados.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(capturados.list.get(0).getFormattedMessage()).contains("rechazada").contains("invalid_client")
                .contains("voting_api");
    }

    @Test
    @DisplayName("Un rechazo de un cliente desconocido registra una línea WARN con 'cliente desconocido'")
    void rechazoDesconocido() {
        registro.rechazoClienteDesconocido("invalid_request");

        assertThat(capturados.list).hasSize(1);
        assertThat(capturados.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(capturados.list.get(0).getFormattedMessage()).contains("invalid_request")
                .contains("cliente desconocido");
    }
}
