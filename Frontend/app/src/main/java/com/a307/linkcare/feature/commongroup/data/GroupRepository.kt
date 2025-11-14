package com.a307.linkcare.feature.commongroup.data

import com.a307.linkcare.feature.commongroup.domain.MyGroupResponse
import javax.inject.Inject

class GroupRepository @Inject constructor(
    private val api: GroupApi
) {
    suspend fun getMyGroups(type: String?): List<MyGroupResponse> {
        return api.getMyGroups(type)
    }

    suspend fun getGroupDetail(groupSeq: Long) =
        api.getMyGroupDetail(groupSeq)
}