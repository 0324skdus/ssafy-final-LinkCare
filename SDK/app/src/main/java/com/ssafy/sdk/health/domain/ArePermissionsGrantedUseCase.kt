/*
 * Copyright 2025 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ssafy.sdk.health.domain

import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.error.HealthDataException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import javax.inject.Inject

class ArePermissionsGrantedUseCase @Inject constructor(
    private val healthDataStore: HealthDataStore
) {
    @Throws(HealthDataException::class)
    suspend operator fun invoke(): Boolean {
        val grantedPermissions = healthDataStore.getGrantedPermissions(Permissions.PERMISSIONS)
        val areAllPermissionsGranted = grantedPermissions.containsAll(Permissions.PERMISSIONS)
        return areAllPermissionsGranted
    }
}

/*********************************************************************************
 * [Practice 1] Create permission set to receive step data
 *
 * Make PERMISSIONS set down below contain two Permission
 * (com.samsung.android.sdk.health.data.permission.Permission) objects of types:
 *  - 'DataTypes.STEPS' of 'AccessType.READ'
 *  - 'DataTypes.STEPS_GOAL of 'AccessType.READ'
 *
----------------------------------------------------------------------------------
 *
 *  - (Hint)
 *  use Permission.of() function to define the permission types
 *********************************************************************************/
object Permissions {

    val PERMISSIONS = setOf<Permission> (
        // 일일 걸음수
        Permission.of(DataTypes.STEPS, AccessType.READ),
        Permission.of(DataTypes.STEPS_GOAL, AccessType.READ),

        // 활동 요약
        Permission.of(DataTypes.ACTIVITY_SUMMARY, AccessType.READ),

        // 심박수
        Permission.of(DataTypes.HEART_RATE, AccessType.READ),

        // 음수량
        Permission.of(DataTypes.WATER_INTAKE, AccessType.READ),
        Permission.of(DataTypes.WATER_INTAKE_GOAL, AccessType.READ),

        // 수면
        Permission.of(DataTypes.SLEEP, AccessType.READ),
        Permission.of(DataTypes.SLEEP_GOAL, AccessType.READ),

        // 운동
        Permission.of(DataTypes.EXERCISE, AccessType.READ),

        // 혈압
        Permission.of(DataTypes.BLOOD_PRESSURE, AccessType.READ),


    )
}