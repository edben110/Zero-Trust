package com.zerotrust.voteprocessor.port;

import com.zerotrust.voteprocessor.domain.CandidateResult;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;

import java.util.List;

public interface ConteoVotos {

    void registrar(RawVoteEvent voto);

    List<CandidateResult> conteoActual();
}
