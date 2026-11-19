package com.zerotrust.regionalrollup.port;

import com.zerotrust.regionalrollup.domain.RegionalResultSnapshot;

/**
 * Puerto de salida: publica snapshots al Topic Exchange live_results_regional.
 */
public interface RegionalResultPublisher {
    void publish(RegionalResultSnapshot snapshot);
}
