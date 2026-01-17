package tech.grastone.fz.matching.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.enums.ConnectionStatus;

@Getter
@Setter
@Entity
@Table(name = "connections",
    indexes = {
        @Index(name = "idx_user_id_1", columnList = "userId1"),
        @Index(name = "idx_user_id_2", columnList = "userId2")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "userId1", "userId2" })
    })
public class ConnectionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId1;

    @Column(nullable = false)
    private Long userId2;

    @Column(nullable = false)
    private LocalDateTime connectedAt;

    private boolean isActive = true;

    // NEW FIELDS
    @Column(nullable = false)
    private Long sentBy;

    private Long acceptedBy;

    private Long removedBy;

    private LocalDateTime removedAt;

    @Enumerated(EnumType.STRING)
	private ConnectionStatus status;

    private String remarks;
}
