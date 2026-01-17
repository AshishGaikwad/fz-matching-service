package tech.grastone.fz.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.PreferencesWomenEntity;


@Repository
public interface PreferencesWomenRepository extends JpaRepository<PreferencesWomenEntity, Long>{
	
	
//	@Query( nativeQuery = true, value="""
//				select
//					u.id,
//					(6371 * ACOS(
//				COS(RADIANS(-65.963458)) * COS(RADIANS(u.lattitude)) *
//				COS(RADIANS(u.longitude) - RADIANS(80.289978)) +
//				SIN(RADIANS(-65.963458)) * SIN(RADIANS(u.lattitude))
//				)) as distance_km,
//					(((case
//						when pw.drinking = 2 then 6
//						else 0
//					end)+(case
//						when pw.lifestyle = 2 then 17
//						else 0
//					end)+(case
//						when pw.religion = 3 then 3
//						else 0
//					end)+(case
//						when pw.smoking = 0 then 9
//						else 0
//					end)+(case
//						when pw.personality = 1 then 12
//						else 0
//					end))/ 47)* 100 as matching_per
//				from
//					preferences_women pw
//				inner join users u
//				on
//					pw.user_id = u.id
//				where
//					(pw.drinking = 2
//						or pw.lifestyle = 2
//						or pw.religion = 3
//						or pw.smoking = 0
//						or pw.personality = 1)
//						AND TIMESTAMPDIFF(YEAR, u.dob, CURDATE()) BETWEEN 18 AND 68
//				having
//					distance_km < 300000
//				order by
//					id asc;
//			""" )
//	public List<Object[]> getMatchedUserUsingPreferences();

}
