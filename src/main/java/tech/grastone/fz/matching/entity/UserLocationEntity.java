package tech.grastone.fz.matching.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(
        name = "user_locations",
        indexes = {
                @Index(name = "idx_user_locations_updated", columnList = "updated_at"),
                @Index(name = "idx_user_locations_geo", columnList = "latitude,longitude")
        }
)
public class UserLocationEntity extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, columnDefinition = "DECIMAL(9,6)")
    private double latitude;

    @Column(nullable = false, columnDefinition = "DECIMAL(9,6)")
    private double longitude;

    @Column(name = "accuracy_meters")
    private Integer accuracyMeters;

    @Column(length = 32)
    private String source;

    @Transient
    public LocalDateTime getLastUpdatedAt() {
        return getUpdatedAt();
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        setUpdatedAt(lastUpdatedAt);
    }
}
