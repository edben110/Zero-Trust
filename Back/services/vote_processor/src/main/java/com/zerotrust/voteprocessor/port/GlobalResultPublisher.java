package com.zerotrust.voteprocessor.port;

import com.zerotrust.voteprocessor.domain.GlobalResultSnapshot;

public interface GlobalResultPublisher {
    void publish(GlobalResultSnapshot snapshot);
}
