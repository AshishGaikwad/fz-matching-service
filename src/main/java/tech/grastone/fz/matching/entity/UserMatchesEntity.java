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
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.MatchStatus;
import tech.grastone.fz.matching.enums.MatchType;

@Entity
@Table(name = "user_matches", indexes = { @Index(name = "idx_match_users", columnList = "userId1,userId2"),
		@Index(name = "idx_user_id_1", columnList = "userId1"),
		@Index(name = "idx_user_id_2", columnList = "userId2") }, uniqueConstraints = {
				@UniqueConstraint(columnNames = "userId1,userId2,matchType") })
@Getter
@Setter
public class UserMatchesEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private long userId1;

	@Column(nullable = false)
	private long userId2;

	@Enumerated(EnumType.ORDINAL)
	private MatchStatus matchStatus;

	@Enumerated(EnumType.ORDINAL)
	private MatchType matchType;

}
