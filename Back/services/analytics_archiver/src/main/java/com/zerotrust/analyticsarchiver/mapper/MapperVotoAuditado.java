package com.zerotrust.analyticsarchiver.mapper;

import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import com.zerotrust.analyticsarchiver.repository.entity.VotoAuditadoEntity;

public final class MapperVotoAuditado {

    private MapperVotoAuditado() {
    }

    public static VotoAuditadoEntity aEntidad(VotoAuditado voto) {
        return new VotoAuditadoEntity(voto.voteId().valor(), voto.voterId().valor(), voto.candidateId().valor(),
                voto.region().valor(), voto.sourceIp().valor(), voto.votadoEn());
    }

    public static VotoAuditado aDominio(VotoAuditadoEntity entidad) {
        return new VotoAuditado(new VoteId(entidad.getVoteId()), new VoterId(entidad.getVoterId()),
                new CandidateId(entidad.getCandidateId()), new Region(entidad.getRegion()),
                new SourceIp(entidad.getSourceIp()), entidad.getVotedAt());
    }
}
