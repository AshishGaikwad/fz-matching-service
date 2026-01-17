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
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.RequestStatus;

@Getter
@Setter
@Entity
@Table(name = "match_request", indexes = { @Index(name = "idx_sender_id", columnList = "senderId"),
		@Index(name = "idx_receiver_id", columnList = "receiverId") }, uniqueConstraints = {
				@UniqueConstraint(columnNames = { "senderId", "receiverId" }) })

public class MatchRequestEntity extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long matchRequestId;

	@Column(nullable = false)
	private Long senderId;

	@Column(nullable = false)
	private Long receiverId;

	@Column(length = 100)
	private String requestMessage;

	@Column(length = 100)
	private String replyMessage;

	@Enumerated(EnumType.ORDINAL)
	private RequestStatus requestStatus;

}
