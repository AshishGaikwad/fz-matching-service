package tech.grastone.fz.matching.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;


@Table(name="PREFERENCES_GAY",
indexes = {
		@Index(name="idx_age",columnList = "minAge,maxAge"),
		@Index(name="idx_distance",columnList = "distance"),
		@Index(name="idx_personality",columnList = "personality"),
		@Index(name="idx_religion",columnList = "religion"),
		@Index(name="idx_lifestyle",columnList = "lifestyle"),
		@Index(name="idx_marital_status",columnList = "marital_status"),
		@Index(name="idx_profession",columnList = "profession"),
		@Index(name="idx_looking_for",columnList = "looking_for")
})
@Entity
public class PreferencesGayEntity extends BasePreferenceEntity {

}
