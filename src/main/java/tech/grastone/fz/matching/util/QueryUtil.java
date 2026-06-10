package tech.grastone.fz.matching.util;

import org.springframework.stereotype.Component;

@Component
public class QueryUtil {

	public String getQueryForMatchedUserUsingPreferences(String tableName) {
		return """
					SELECT
                     u.id,
                     (
                          6371 * ACOS(
                             COS(RADIANS(:lat_rad)) * COS(RADIANS(u.lattitude)) *
                             COS(RADIANS(u.longitude) - RADIANS(:lon_rad)) +
                             SIN(RADIANS(:lat_rad)) * SIN(RADIANS(u.lattitude))
                         )
                     ) AS distance_km,
                     (
                         (
                             (CASE WHEN pw.drinking = :drinking THEN 6 ELSE 0 END) +
                             (CASE WHEN pw.lifestyle = :lifestyle THEN 17 ELSE 0 END) +
                             (CASE WHEN pw.religion = :religion THEN 3 ELSE 0 END) +
                             (CASE WHEN pw.smoking = :smoking THEN 9 ELSE 0 END) +
                             (CASE WHEN pw.personality = :personality THEN 12 ELSE 0 END)
                         ) / 47.0
                     ) * 100 AS matching_per
                 FROM
                     {{tableName}} pw
                 JOIN users u ON pw.user_id = u.id
                 WHERE
                     (
                         pw.drinking = :drinking OR
                         pw.lifestyle = :lifestyle OR
                         pw.religion = :religion OR
                         pw.smoking = :smoking OR
                         pw.personality = :personality
                     )
                     AND TIMESTAMPDIFF(YEAR, u.dob, CURDATE()) BETWEEN :minAge AND :maxAge
                 
                     AND NOT EXISTS (
                         SELECT 1
                         FROM user_matches um
                         WHERE (
                             (um.user_id1 = :currentUserId AND um.user_id2 = u.id)
                         )
                         AND (
                             um.match_status = 0 OR
                             (um.match_status = 1 AND TIMESTAMPDIFF(HOUR, um.updated_at, NOW()) < 24)
                         )
                     )
                 
                     AND NOT EXISTS (
                         SELECT 1
                         FROM match_request mr
                         WHERE (mr.sender_id = :currentUserId AND mr.receiver_id = u.id) or
                         (mr.sender_id = u.id AND mr.receiver_id = :currentUserId)
                     )
                 
                     AND NOT EXISTS (
                         SELECT 1
                         FROM connections c
                         WHERE c.user_id1 = LEAST(:currentUserId, u.id)
                           AND c.user_id2 = GREATEST(:currentUserId, u.id)
                     )
                 
                     AND (
                         6371 * ACOS(
                             COS(RADIANS(:lat_rad)) * COS(RADIANS(u.lattitude)) *
                             COS(RADIANS(u.longitude) - RADIANS(:lon_rad)) +
                             SIN(RADIANS(:lat_rad)) * SIN(RADIANS(u.lattitude))
                         )
                     ) < 300
                     
                  
                 
                 ORDER BY
                     u.id DESC
				""".replace("{{tableName}}", tableName);
	}

}
