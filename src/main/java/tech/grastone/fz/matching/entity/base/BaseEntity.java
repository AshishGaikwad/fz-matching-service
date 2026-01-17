package tech.grastone.fz.matching.entity.base;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public class BaseEntity {

    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private String updatedBy;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
