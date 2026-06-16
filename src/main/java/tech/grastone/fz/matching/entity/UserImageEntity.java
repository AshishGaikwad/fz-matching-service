package tech.grastone.fz.matching.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import tech.grastone.fz.matching.entity.base.BaseEntity;
import tech.grastone.fz.matching.enums.ImageType;
import tech.grastone.fz.matching.enums.ImageModerationStatus;

@Entity
@Table(name="user_images", uniqueConstraints = {
		@UniqueConstraint(columnNames = "userId,type,url") })
@Getter
@Setter
public class UserImageEntity extends BaseEntity implements Serializable{

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
	
	private long userId;
	
	@Enumerated(EnumType.ORDINAL)
	private ImageType type;
	
	private String url;

	@Enumerated(EnumType.STRING)
	private ImageModerationStatus moderationStatus;

	@Column(length = 512)
	private String moderationNotes;

	private LocalDateTime moderatedAt;
}
