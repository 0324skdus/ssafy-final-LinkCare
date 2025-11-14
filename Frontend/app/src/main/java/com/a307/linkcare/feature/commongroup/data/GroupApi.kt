package com.a307.linkcare.feature.commongroup.data

import com.a307.linkcare.feature.commongroup.domain.GroupDetailResponse
import com.a307.linkcare.feature.commongroup.domain.MyGroupResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GroupApi {
    @GET("/api/groups/my")
    suspend fun getMyGroups(
        @Query("type") type: String? = null   // HEALTH, CARE, 혹은 null
    ): List<MyGroupResponse>

    @GET("/api/groups/{groupSeq}")
    suspend fun getMyGroupDetail(
        @Path("groupSeq") groupSeq: Long
    ): GroupDetailResponse
}