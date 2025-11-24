package com.a307.linkcare.feature.commongroup.data.api

import com.a307.linkcare.feature.commongroup.data.model.response.GroupCommentsResponse
import com.a307.linkcare.feature.commongroup.data.model.response.GroupDetailResponse
import com.a307.linkcare.feature.commongroup.data.model.response.GroupResponse
import com.a307.linkcare.feature.commongroup.data.model.request.GroupTotalActivityStats
import com.a307.linkcare.feature.commongroup.data.model.response.InvitationPreviewResponse
import com.a307.linkcare.feature.commongroup.data.model.response.InvitationResponse
import com.a307.linkcare.feature.commongroup.data.model.response.JoinRequestResponse
import com.a307.linkcare.feature.commongroup.data.model.response.MyGroupResponse
import com.a307.linkcare.feature.commongroup.data.model.dto.PermissionAgreementDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface GroupApi {
    @GET("/api/groups/my")
    suspend fun getMyGroups(
        @Query("type") type: String? = null   // HEALTH, CARE, 혹은 null
    ): List<MyGroupResponse>

    @GET("/api/groups/my-pending")
    suspend fun getMyPendingGroups(
        @Query("type") type: String? = null   // HEALTH, CARE, 혹은 null
    ): List<MyGroupResponse>

    @GET("/api/groups/{groupSeq}")
    suspend fun getMyGroupDetail(
        @Path("groupSeq") groupSeq: Long
    ): GroupDetailResponse

    @POST("/api/groups/{groupSeq}/invitations")
    suspend fun createInvitation(
        @Path("groupSeq") groupSeq: Long
    ): Response<InvitationResponse>

    // 초대 링크 미리보기
    @GET("/api/groups/invitations/{token}/preview")
    suspend fun getInvitationPreview(
        @Path("token") token: String
    ): Response<InvitationPreviewResponse>

    // 초대 링크로 그룹 참가 신청
    @POST("/api/groups/invitations/{token}/join")
    suspend fun joinGroupByInvitation(
        @Path("token") token: String,
        @Body permissions: PermissionAgreementDto
    ): Response<Unit>

    // 그룹 검색
    @GET("/api/groups/search")
    suspend fun searchGroups(
        @Query("keyword") keyword: String
    ): Response<List<MyGroupResponse>>

    // 검색으로 그룹 참가 신청
    @POST("/api/groups/{groupSeq}/join")
    suspend fun joinGroupBySearch(
        @Path("groupSeq") groupSeq: Long,
        @Body permissions: PermissionAgreementDto
    ): Response<Unit>

    // 대기 중인 참가 신청 목록 조회 (방장 전용)
    @GET("/api/groups/{groupSeq}/join-requests")
    suspend fun getPendingJoinRequests(
        @Path("groupSeq") groupSeq: Long
    ): Response<List<JoinRequestResponse>>

    // 참가 신청 승인 (방장 전용)
    @POST("/api/groups/join-requests/{requestSeq}/approve")
    suspend fun approveJoinRequest(
        @Path("requestSeq") requestSeq: Long
    ): Response<Unit>

    // 참가 신청 거절 (방장 전용)
    @POST("/api/groups/join-requests/{requestSeq}/reject")
    suspend fun rejectJoinRequest(
        @Path("requestSeq") requestSeq: Long
    ): Response<Unit>

    // 그룹 정보 수정 (방장 전용)
    @Multipart
    @PUT("/api/groups/{groupSeq}")
    suspend fun updateGroup(
        @Path("groupSeq") groupSeq: Long,
        @Part("groupName") groupName: RequestBody,
        @Part("groupDescription") groupDescription: RequestBody,
        @Part("imageAction") imageAction: RequestBody, // "keep", "delete", "update"
        @Part image: MultipartBody.Part?,
        @Part("isSleepRequired") isSleepRequired: RequestBody?,
        @Part("isWaterIntakeRequired") isWaterIntakeRequired: RequestBody?,
        @Part("isBloodPressureRequired") isBloodPressureRequired: RequestBody?,
        @Part("isBloodSugarRequired") isBloodSugarRequired: RequestBody?,
        @Part("minCalorie") minCalorie: RequestBody?,
        @Part("minStep") minStep: RequestBody?,
        @Part("minDistance") minDistance: RequestBody?,
        @Part("minDuration") minDuration: RequestBody?
    ): Response<MyGroupResponse>

    // 그룹원 권한 동의 (케어 그룹 전용)
    @POST("/api/groups/{groupSeq}/permissions/agree")
    suspend fun agreeToGroupPermissions(
        @Path("groupSeq") groupSeq: Long,
        @Body agreement: PermissionAgreementDto
    ): Response<Unit>

    // 그룹원 권한 자동 동의 (케어 그룹 전용)
    @POST("/api/groups/{groupSeq}/permissions/auto-agree")
    suspend fun autoAgreeToGroupPermissions(
        @Path("groupSeq") groupSeq: Long
    ): Response<Unit>

    // 그룹 이번주 통계
    @GET("/api/groups/{groupSeq}/total-acitivty/stats")
    suspend fun getTotalActivityStats(
        @Path("groupSeq") groupSeq: Long,
        @Query("startDate") startDate: String, // yyyy-MM-dd
        @Query("endDate") endDate: String      // yyyy-MM-dd
    ): GroupTotalActivityStats

    // 그룹장 위임하기
    @POST("/api/groups/{groupSeq}/delegate")
    suspend fun delegateLeader(
        @Path("groupSeq") groupSeq: Long,
        @Query("newLeaderUserSeq") newLeaderUserSeq: Long
    ): Response<Unit>

    // 그룹원 내보내기
    @DELETE("/api/groups/{groupSeq}/members/{targetUserSeq}")
    suspend fun kickMember(
        @Path("groupSeq") groupSeq: Long,
        @Path("targetUserSeq") targetUserSeq: Long
    ): Response<Unit>

    // 그룹 탈퇴
    @DELETE("/api/groups/{groupSeq}/leave")
    suspend fun leaveGroup(
        @Path("groupSeq") groupSeq: Long
    ): Response<Unit>

    // 그룹 삭제
    @DELETE("/api/groups/{groupSeq}")
    suspend fun deleteGroup(
        @Path("groupSeq") groupSeq: Long
    ): Response<Unit>

    // 그룹 멤버 한줄평 조회
    @GET("/api/ai/comments/group/{groupSeq}")
    suspend fun getGroupMemberComments(
        @Path("groupSeq") groupSeq: Long
    ): GroupCommentsResponse

    // 그룹 status 별 조회
    @GET("/api/groups")
    suspend fun getAllGroups(
        @Query("type") type: String? = null
    ): List<GroupResponse>
}