package com.ssafy.linkcare.ai.comment.repository;

import com.ssafy.linkcare.ai.comment.entity.UserAiComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * AI 한줄평 Repository
 */
public interface UserAiCommentRepository extends JpaRepository<UserAiComment, Long> {

    /**
     * 특정 사용자의 특정 그룹 한줄평 조회
     *
     * @param userPk 사용자 PK
     * @param groupSeq 그룹 PK
     * @return 한줄평 Optional
     */
    Optional<UserAiComment> findByUser_UserPkAndGroup_GroupSeq(Long userPk, Long groupSeq);

    /**
     * 여러 사용자의 특정 그룹 한줄평 일괄 조회
     *
     * @param userPks 사용자 PK 목록
     * @param groupSeq 그룹 PK
     * @return 한줄평 목록
     */
    @Query("SELECT c FROM UserAiComment c WHERE c.user.userPk IN :userPks AND c.group.groupSeq = :groupSeq")
    List<UserAiComment> findByUserPksAndGroup_GroupSeq(
        @Param("userPks") List<Long> userPks,
        @Param("groupSeq") Long groupSeq
    );

    /**
     * 특정 그룹의 모든 한줄평 조회
     *
     * @param groupSeq 그룹 PK
     * @return 한줄평 목록
     */
    @Query("SELECT c FROM UserAiComment c WHERE c.group.groupSeq = :groupSeq ORDER BY c.updatedAt DESC")
    List<UserAiComment> findByGroup_GroupSeqOrderByUpdatedAtDesc(@Param("groupSeq") Long groupSeq);
}
