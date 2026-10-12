package tech.grastone.fz.matching.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.grastone.fz.matching.entity.UserRelationshipEntity;
import tech.grastone.fz.matching.enums.RelationshipStatus;

@Repository
public interface UserRelationshipRepository extends JpaRepository<UserRelationshipEntity, Long> {

    Optional<UserRelationshipEntity> findByUserId1AndUserId2(Long userId1, Long userId2);

    @Query("""
            select r
              from UserRelationshipEntity r
             where (r.userId1 = :userId or r.userId2 = :userId)
               and r.status in :statuses
            """)
    List<UserRelationshipEntity> findForUserByStatuses(
            @Param("userId") Long userId,
            @Param("statuses") Collection<RelationshipStatus> statuses
    );

    List<UserRelationshipEntity> findByInitiatedByAndStatus(Long initiatedBy, RelationshipStatus status);

    List<UserRelationshipEntity> findByTargetUserIdAndStatus(Long targetUserId, RelationshipStatus status);

    @Query("""
            select r
              from UserRelationshipEntity r
             where (
                       (r.userId1 = :userId and r.userId2 in :candidateIds)
                    or (r.userId2 = :userId and r.userId1 in :candidateIds)
                   )
            """)
    List<UserRelationshipEntity> findForUserAndCandidates(
            @Param("userId") Long userId,
            @Param("candidateIds") Collection<Long> candidateIds
    );
}
