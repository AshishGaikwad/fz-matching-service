package tech.grastone.fz.matching.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import tech.grastone.fz.matching.entity.base.BasePreferenceEntity;


@Table(name="PREFERENCES_LESBIAN",
indexes = {
		@Index(name="idx_age",columnList = "minAge,maxAge"),
		@Index(name="idx_distance",columnList = "distance"),
		@Index(name="idx_personality",columnList = "personality"),
		@Index(name="idx_religion",columnList = "religion"),
		@Index(name="idx_lifestyle",columnList = "lifestyle")
})
@Entity
public class PreferencesLesbianEntity extends BasePreferenceEntity {

}
