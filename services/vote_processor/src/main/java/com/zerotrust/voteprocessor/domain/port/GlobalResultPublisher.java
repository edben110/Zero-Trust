package com.zerotrust.voteprocessor.domain.port;

import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;

/**
 * Puerto de salida: publica snapshots al Fanout Exchange live_results_global.
 * El dominio depende de esta abstracción, nunca del adaptador concreto de RabbitMQ.
 */
public interface GlobalResultPublisher {
    void publish(GlobalResultSnapshot snapshot);
}
