package com.a307.linkcare.feature.commongroup.domain.repository

import android.util.Log
import com.a307.linkcare.feature.commongroup.data.api.GroupApi
import com.a307.linkcare.feature.commongroup.data.model.response.GroupResponse
import com.a307.linkcare.feature.commongroup.data.model.request.GroupTotalActivityStats
import com.a307.linkcare.feature.commongroup.data.model.response.InvitationPreviewResponse
import com.a307.linkcare.feature.commongroup.data.model.response.InvitationResponse
import com.a307.linkcare.feature.commongroup.data.model.response.JoinRequestResponse
import com.a307.linkcare.feature.commongroup.data.model.response.MyGroupResponse
import com.a307.linkcare.feature.commongroup.data.model.dto.PermissionAgreementDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val api: GroupApi
) {
    suspend fun getMyGroups(type: String?): List<MyGroupResponse> {
        return try {
            val result = api.getMyGroups(type)
            Log.d("GroupRepository", "내 그룹 목록 조회 성공: type=$type, count=${result.size}")
            result
        } catch (e: Exception) {
            Log.e("GroupRepository", "내 그룹 목록 조회 실패: type=$type, error=${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMyPendingGroups(type: String?): List<MyGroupResponse> {
        return try {
            val result = api.getMyPendingGroups(type)
            Log.d("GroupRepository", "신청 그룹 목록 조회 성공: type=$type, count=${result.size}")
            result
        } catch (e: Exception) {
            Log.e("GroupRepository", "신청 그룹 목록 조회 실패: type=$type, error=${e.message}", e)
            emptyList()
        }
    }

    suspend fun getGroupDetail(groupSeq: Long) =
        api.getMyGroupDetail(groupSeq)

    suspend fun createInvitation(groupSeq: Long): Result<InvitationResponse> {
        return try {
            val response = api.createInvitation(groupSeq)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create invitation: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 초대 링크 미리보기
    suspend fun getInvitationPreview(token: String): Result<InvitationPreviewResponse> {
        return try {
            val response = api.getInvitationPreview(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get invitation preview: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 초대 링크로 그룹 참가
    suspend fun joinGroupByInvitation(token: String, permissions: PermissionAgreementDto): Result<Unit> {
        return try {
            val response = api.joinGroupByInvitation(token, permissions)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to join group: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 그룹 검색
    suspend fun searchGroups(keyword: String): Result<List<MyGroupResponse>> {
        return try {
            val response = api.searchGroups(keyword)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to search groups: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 검색으로 그룹 참가 신청
    suspend fun joinGroupBySearch(groupSeq: Long, permissions: PermissionAgreementDto): Result<Unit> {
        return try {
            val response = api.joinGroupBySearch(groupSeq, permissions)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to join group: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 대기 중인 참가 신청 목록 조회
    suspend fun getPendingJoinRequests(groupSeq: Long): Result<List<JoinRequestResponse>> {
        return try {
            val response = api.getPendingJoinRequests(groupSeq)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get join requests: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 참가 신청 승인
    suspend fun approveJoinRequest(requestSeq: Long): Result<Unit> {
        return try {
            val response = api.approveJoinRequest(requestSeq)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to approve join request: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 참가 신청 거절
    suspend fun rejectJoinRequest(requestSeq: Long): Result<Unit> {
        return try {
            val response = api.rejectJoinRequest(requestSeq)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to reject join request: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 그룹 정보 수정
    suspend fun updateGroup(
        groupSeq: Long,
        groupName: String,
        groupDescription: String,
        imageAction: String, // "keep", "delete", "update"
        imageFile: File? = null,
        isSleepRequired: Boolean? = null,
        isWaterIntakeRequired: Boolean? = null,
        isBloodPressureRequired: Boolean? = null,
        isBloodSugarRequired: Boolean? = null,
        minCalorie: Float? = null,
        minStep: Int? = null,
        minDistance: Float? = null,
        minDuration: Int? = null
    ): Result<MyGroupResponse> {
        return try {
            val groupNamePart = groupName.toRequestBody("text/plain".toMediaTypeOrNull())
            val groupDescriptionPart = groupDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            val imageActionPart = imageAction.toRequestBody("text/plain".toMediaTypeOrNull())

            val imagePart = imageFile?.let {
                val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", it.name, requestFile)
            }

            val sleepPart = isSleepRequired?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val waterPart = isWaterIntakeRequired?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val bloodPressurePart = isBloodPressureRequired?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val bloodSugarPart = isBloodSugarRequired?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val caloriePart = minCalorie?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val stepPart = minStep?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val distancePart = minDistance?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val durationPart = minDuration?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.updateGroup(
                groupSeq = groupSeq,
                groupName = groupNamePart,
                groupDescription = groupDescriptionPart,
                imageAction = imageActionPart,
                image = imagePart,
                isSleepRequired = sleepPart,
                isWaterIntakeRequired = waterPart,
                isBloodPressureRequired = bloodPressurePart,
                isBloodSugarRequired = bloodSugarPart,
                minCalorie = caloriePart,
                minStep = stepPart,
                minDistance = distancePart,
                minDuration = durationPart
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update group: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 그룹원 권한 동의
    suspend fun agreeToGroupPermissions(groupSeq: Long, agreement: PermissionAgreementDto): Result<Unit> {
        return try {
            val response = api.agreeToGroupPermissions(groupSeq, agreement)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to agree to permissions: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 그룹원 권한 자동 동의
    suspend fun autoAgreeToGroupPermissions(groupSeq: Long): Result<Unit> {
        return try {
            val response = api.autoAgreeToGroupPermissions(groupSeq)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to auto agree to permissions: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 그룹 이번주 운동 통계
    suspend fun getTotalActivityStats(
        groupSeq: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): GroupTotalActivityStats {
        return api.getTotalActivityStats(
            groupSeq = groupSeq,
            startDate = startDate.toString(),
            endDate = endDate.toString()
        )
    }

    // 그룹장 위임하기
    suspend fun delegateLeader(groupSeq: Long, newLeaderUserSeq: Long): Result<Unit> {
        return try {
            val response = api.delegateLeader(groupSeq, newLeaderUserSeq)
            if (response.isSuccessful) {
                Log.d("GroupRepository", "그룹장 위임 성공: groupSeq=$groupSeq, newLeader=$newLeaderUserSeq")
                Result.success(Unit)
            } else {
                Log.e("GroupRepository", "그룹장 위임 실패: HTTP ${response.code()}")
                Result.failure(Exception("Failed to delegate leader: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "그룹장 위임 예외: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 그룹원 내보내기
    suspend fun kickMember(groupSeq: Long, targetUserSeq: Long): Result<Unit> {
        return try {
            val response = api.kickMember(groupSeq, targetUserSeq)
            if (response.isSuccessful) {
                Log.d("GroupRepository", "그룹원 내보내기 성공: groupSeq=$groupSeq, target=$targetUserSeq")
                Result.success(Unit)
            } else {
                Log.e("GroupRepository", "그룹원 내보내기 실패: HTTP ${response.code()}")
                Result.failure(Exception("Failed to kick member: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "그룹원 내보내기 예외: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 그룹 탈퇴
    suspend fun leaveGroup(groupSeq: Long): Result<Unit> {
        return try {
            val response = api.leaveGroup(groupSeq)
            if (response.isSuccessful) {
                Log.d("GroupRepository", "그룹 탈퇴 성공: groupSeq=$groupSeq")
                Result.success(Unit)
            } else {
                Log.e("GroupRepository", "그룹 탈퇴 실패: HTTP ${response.code()}")
                Result.failure(Exception("Failed to leave group: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "그룹 탈퇴 예외: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 그룹 삭제
    suspend fun deleteGroup(groupSeq: Long): Result<Unit> {
        return try {
            val response = api.deleteGroup(groupSeq)
            if (response.isSuccessful) {
                Log.d("GroupRepository", "그룹 삭제 성공: groupSeq=$groupSeq")
                Result.success(Unit)
            } else {
                Log.e("GroupRepository", "그룹 삭제 실패: HTTP ${response.code()}")
                Result.failure(Exception("Failed to delete group: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("GroupRepository", "그룹 삭제 예외: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 그룹 전체 조회
    suspend fun getAllGroups(type: String?): List<GroupResponse> {
        return try {
            val result = api.getAllGroups(type)
            Log.d("GroupRepository", "내 그룹 목록 조회 성공: type=$type, count=${result.size}")
            result
        } catch (e: Exception) {
            Log.e("GroupRepository", "내 그룹 목록 조회 실패: type=$type, error=${e.message}", e)
            emptyList()
        }
    }

    // 그룹 멤버 한줄평 조회
    suspend fun getGroupMemberComments(groupSeq: Long) =
        api.getGroupMemberComments(groupSeq)
}