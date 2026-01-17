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
import tech.grastone.fz.matching.enums.Frequency;
import tech.grastone.fz.matching.enums.LimitType;

@Getter
@Setter
@Entity
@Table(name = "user_limits", indexes = { @Index(name = "idx_user_id_1", columnList = "userId"),
		@Index(name = "idx_user_limit", columnList = "userId,frequency,limitType,") }, uniqueConstraints = {
				@UniqueConstraint(columnNames = "userId,frequency,limitType,preriodKey") })
public class UserLimitsEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private long userId;
	
	@Enumerated(EnumType.ORDINAL)
	private Frequency frequency;
	
	@Enumerated(EnumType.ORDINAL)
	private LimitType limitType;
	
	@Column(nullable = false)
	private int usageCount;
	
	@Column(nullable = false)
	private int limitValue;
	
	@Column(length = 10, nullable = false)
	private String periodKey;
	
}
