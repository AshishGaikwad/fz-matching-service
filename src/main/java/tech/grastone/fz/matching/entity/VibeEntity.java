package tech.grastone.fz.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.VibeActivityType;

@Getter
@Setter
@Entity
@Table(name = "vibes", indexes = {
        @Index(name = "idx_vibes_activity", columnList = "activity_type"),
        @Index(name = "idx_vibes_active_sort", columnList = "active,sort_order")
})
public class VibeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vibe_id")
    private Long vibeId;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false, length = 40)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 32)
    private VibeActivityType activityType;

    @Column(name = "default_duration_minutes", nullable = false)
    private int defaultDurationMinutes = 60;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
