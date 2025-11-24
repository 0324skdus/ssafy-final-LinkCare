package com.a307.linkcare.feature.commongroup.data.model.request

import com.a307.linkcare.feature.commongroup.domain.model.Member

data class MemberDetailInfo(
    val member: Member,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val petName: String?
)