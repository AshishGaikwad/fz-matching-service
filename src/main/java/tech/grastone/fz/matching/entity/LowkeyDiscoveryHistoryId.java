package tech.grastone.fz.matching.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class LowkeyDiscoveryHistoryId implements Serializable {

    @Column(name = "viewer_user_id")
    private Long viewerUserId;

    @Column(name = "candidate_user_id")
    private Long candidateUserId;
}
