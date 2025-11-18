package com.ssafy.linkcare.ai.comment.service;

import com.ssafy.linkcare.ai.comment.dto.BatchCommentsResponse;
import com.ssafy.linkcare.ai.comment.dto.CommentUploadRequest;
import com.ssafy.linkcare.ai.comment.dto.CommentUploadResponse;
import com.ssafy.linkcare.ai.comment.dto.UserCommentDto;
import com.ssafy.linkcare.ai.comment.entity.UserAiComment;
import com.ssafy.linkcare.ai.comment.repository.UserAiCommentRepository;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.group.repository.GroupMemberRepository;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 한줄평 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommentService {

    private final UserAiCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * 한줄평 업로드/갱신 (UPSERT)
     * - 기존 한줄평이 있으면 UPDATE
     * - 없으면 INSERT
     *
     * @param userPk 사용자 PK
     * @param request 한줄평 요청 (groupSeq, comment)
     * @return 저장된 한줄평 응답
     * @throws CustomException USER_NOT_FOUND - 사용자를 찾을 수 없음
     * @throws CustomException GROUP_NOT_FOUND - 그룹을 찾을 수 없음
     * @throws CustomException NOT_GROUP_MEMBER_FOR_COMMENT - 그룹 멤버가 아님
     */
    @Transactional
    public CommentUploadResponse upsertComment(Long userPk, CommentUploadRequest request) {
        log.info("한줄평 UPSERT - userPk: {}, groupSeq: {}", userPk, request.getGroupSeq());

        // 1. 사용자 조회
        User user = userRepository.findById(userPk)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 조회
        Group group = groupRepository.findById(request.getGroupSeq())
            .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 3. 그룹 멤버 여부 확인
        boolean isMember = groupMemberRepository.existsByGroupAndUser_UserPk(group, userPk);
        if (!isMember) {
            log.warn("그룹 멤버가 아닌 사용자의 한줄평 작성 시도 - userPk: {}, groupSeq: {}", userPk, request.getGroupSeq());
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER_FOR_COMMENT);
        }

        // 4. 기존 한줄평 조회 후 UPSERT
        UserAiComment savedComment = commentRepository
            .findByUser_UserPkAndGroup_GroupSeq(userPk, request.getGroupSeq())
            .map(existing -> {
                // UPDATE: 기존 한줄평 수정
                log.info("기존 한줄평 UPDATE - commentSeq: {}", existing.getUserAiCommentSeq());
                existing.updateComment(request.getComment());
                return existing;
            })
            .orElseGet(() -> {
                // INSERT: 새 한줄평 생성
                log.info("새 한줄평 INSERT - userPk: {}, groupSeq: {}", userPk, request.getGroupSeq());
                UserAiComment newComment = UserAiComment.builder()
                    .user(user)
                    .group(group)
                    .comment(request.getComment())
                    .build();
                return commentRepository.save(newComment);
            });

        log.info("한줄평 저장 완료 - userPk: {}, groupSeq: {}, commentSeq: {}",
                userPk, request.getGroupSeq(), savedComment.getUserAiCommentSeq());

        return CommentUploadResponse.builder()
            .userSeq(userPk)
            .groupSeq(savedComment.getGroup().getGroupSeq())
            .comment(savedComment.getComment())
            .updatedAt(savedComment.getUpdatedAt())
            .build();
    }



    /**
     * 그룹의 모든 멤버 한줄평 조회
     * - 코멘트가 없는 멤버도 포함 (comment는 null)
     *
     * @param groupSeq 그룹 PK
     * @return 그룹 멤버별 한줄평 목록
     * @throws CustomException GROUP_NOT_FOUND - 그룹을 찾을 수 없음
     */
    @Transactional(readOnly = true)
    public BatchCommentsResponse getGroupMembersComments(Long groupSeq) {
        log.info("그룹 멤버 한줄평 조회 - groupSeq: {}", groupSeq);

        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
            .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 그룹 멤버 조회 (User 정보 포함)
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupWithUser(group);
        log.info("그룹 멤버 수: {}", groupMembers.size());

        // 3. 그룹의 모든 한줄평 조회
        List<UserAiComment> comments = commentRepository
            .findByGroup_GroupSeqOrderByUpdatedAtDesc(groupSeq);
        log.info("조회된 한줄평 수: {}", comments.size());

        // 4. Map으로 변환 (빠른 검색을 위해)
        Map<Long, UserAiComment> commentMap = comments.stream()
            .collect(Collectors.toMap(c -> c.getUser().getUserPk(), c -> c));

        // 5. DTO 변환 (한줄평 없는 멤버는 comment, updatedAt이 null)
        List<UserCommentDto> result = groupMembers.stream()
            .map(member -> {
                User user = member.getUser();
                UserAiComment comment = commentMap.get(user.getUserPk());
                return UserCommentDto.builder()
                    .userSeq(user.getUserPk())
                    .userName(user.getName())
                    .comment(comment != null ? comment.getComment() : null)
                    .updatedAt(comment != null ? comment.getUpdatedAt() : null)
                    .build();
            })
            .collect(Collectors.toList());

        log.info("그룹 멤버 한줄평 조회 완료 - groupSeq: {}, 총 멤버 수: {}, 한줄평 있는 멤버 수: {}",
                groupSeq, result.size(), comments.size());

        return BatchCommentsResponse.builder()
            .comments(result)
            .build();
    }
}
