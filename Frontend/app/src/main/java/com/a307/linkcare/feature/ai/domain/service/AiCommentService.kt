package com.a307.linkcare.feature.ai.domain.service

import android.app.Application
import android.util.Log
import com.a307.linkcare.common.util.AiResponseValidator
import com.a307.linkcare.common.util.AiLetterResponseValidator
import com.a307.linkcare.common.util.AiNudgeResponseValidator
import com.a307.linkcare.feature.ai.data.api.AiApi
import com.a307.linkcare.feature.ai.data.model.AiCommentRequest
import com.a307.linkcare.feature.ai.data.model.UserHealthStatsResponse
import com.a307.linkcare.feature.ai.domain.model.HealthData
import com.a307.linkcare.feature.ai.domain.model.ModelCategory
import com.a307.linkcare.feature.ai.domain.model.ModelInfo
import com.a307.linkcare.feature.ai.domain.model.ModelPerspective
import com.a307.linkcare.feature.ai.domain.model.ModelRegistry
import com.a307.linkcare.feature.ai.domain.usecase.LoadModelUseCase
import com.a307.linkcare.feature.ai.domain.usecase.SendMessageResult
import com.a307.linkcare.feature.ai.domain.usecase.SendMessageUseCase
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.commongroup.data.GroupRepository
import com.a307.linkcare.feature.commongroup.domain.GoalCriteria
import com.a307.linkcare.feature.commongroup.domain.GroupDetailResponse
import com.a307.linkcare.feature.commongroup.domain.MyGroupResponse
import com.a307.linkcare.feature.healthgroup.data.HealthGroupRepository
import com.a307.linkcare.sdk.health.domain.sync.exercise.ExerciseReader
import com.a307.linkcare.sdk.health.domain.sync.heartRate.HeartRateReader
import com.a307.linkcare.sdk.health.domain.sync.step.StepReader
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 코멘트 생성 및 저장 서비스
 *
 * 5가지 시나리오 처리:
 * ① health-self: 케어 그룹용 본인 건강 상태 요약
 * ② wellness-self: 헬스 그룹용 본인 운동 상태 요약
 * ③ health-other: 케어 그룹 멤버 격려 메시지 (3문장)
 * ④ wellness-other: 헬스 그룹 멤버 격려 메시지 (3문장)
 * ⑤ health_other_short: 케어 그룹 빠른 넛지 메시지
 */
@Singleton
class AiCommentService @Inject constructor(
    private val application: Application,
    private val aiApi: AiApi,
    private val groupRepository: GroupRepository,
    private val healthGroupRepository: HealthGroupRepository,
    private val loadModelUseCase: LoadModelUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val tokenStore: TokenStore,
    private val stepReader: StepReader,
    private val exerciseReader: ExerciseReader,
    private val heartRateReader: HeartRateReader
) {
    private val tag = "AiCommentService"

    /**
     * ① health-self: 케어 그룹용 본인 건강 상태 요약
     * - 모든 케어 그룹에 대해 실행
     * - 각 그룹의 ShareOptions에 따라 데이터 필터링
     * - health_self.gguf 모델 사용
     * - POST /api/ai/comment
     */
    suspend fun generateAndSaveCareGroupComments(): Result<List<String>> {
        return try {
            // 1. 모든 케어 그룹 조회
            val careGroups = groupRepository.getMyGroups("CARE")

            if (careGroups.isEmpty()) {
                return Result.success(emptyList())
            }

            Log.d(tag, "[CARE_SELF] 케어 그룹 ${careGroups.size}개 - 더미 코멘트 생성 시작")

            // 2. 오늘 00:00부터 현재까지의 건강 데이터 수집
            val todayHealthData = collectTodayHealthData()

            val results = mutableListOf<String>()

            // 3. 각 그룹별로 더미 코멘트 생성 및 저장
            for (group in careGroups) {
                // 3-1. 활동 수준 결정
                val activityLevel = determineActivityLevel(todayHealthData)

                // 3-2. 더미 코멘트 생성
                val comment = AiResponseValidator.getDummyResponse(
                    groupType = AiResponseValidator.GroupType.CARE,
                    activityLevel = activityLevel
                )
                Log.d(tag, "[CARE_SELF] 💬 더미 생성: $comment")

                // 3-3. 서버에 저장
                val saveResult = saveCommentToServer(group.groupSeq, comment)
                if (saveResult.isSuccess) {
                    results.add("${group.groupName}: $comment")
                }
            }

            Log.d(tag, "[CARE_SELF] ✅ 완료: ${results.size}/${careGroups.size}개")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(tag, "[CARE_SELF] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ② wellness-self: 헬스 그룹용 본인 운동 상태 요약
     * - 모든 헬스 그룹에 대해 실행
     * - 각 그룹의 selectedMetricType과 1인 하루 목표값 포함
     * - wellness_self.gguf 모델 사용
     * - POST /api/ai/comment
     */
    suspend fun generateAndSaveHealthGroupComments(): Result<List<String>> {
        return try {
            // 1. 모든 헬스 그룹 조회
            val healthGroups = groupRepository.getMyGroups("HEALTH")

            if (healthGroups.isEmpty()) {
                return Result.success(emptyList())
            }

            Log.d(tag, "[WELLNESS_SELF] 헬스 그룹 ${healthGroups.size}개 - 더미 코멘트 생성 시작")

            // 2. 오늘 00:00부터 현재까지의 운동 데이터 수집
            val todayHealthData = collectTodayHealthData()

            val results = mutableListOf<String>()

            // 3. 각 그룹별로 더미 코멘트 생성 및 저장
            for (group in healthGroups) {
                // 3-1. 활동 수준 결정
                val activityLevel = determineActivityLevel(todayHealthData)

                // 3-2. 더미 코멘트 생성
                val comment = AiResponseValidator.getDummyResponse(
                    groupType = AiResponseValidator.GroupType.HEALTH,
                    activityLevel = activityLevel
                )
                Log.d(tag, "[WELLNESS_SELF] 💬 더미 생성: $comment")

                // 3-3. 서버에 저장
                val saveResult = saveCommentToServer(group.groupSeq, comment)
                if (saveResult.isSuccess) {
                    results.add("${group.groupName}: $comment")
                }
            }

            Log.d(tag, "[WELLNESS_SELF] ✅ 완료: ${results.size}/${healthGroups.size}개")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(tag, "[WELLNESS_SELF] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ③ health-other: 케어 그룹 멤버 격려 메시지 (3문장)
     * - 특정 사용자의 건강 데이터 조회
     * - AI 사용하지 않고 5초 로딩 후 더미 데이터 반환
     * - UI에 표시용 (서버 저장 안 함)
     */
    suspend fun generateCareGroupMemberEncouragement(
        userSeq: Long
    ): Result<List<String>> {
        return try {
            Log.d(tag, "[HEALTH_OTHER] ③ 케어 멤버 격려 메시지 생성: userSeq=$userSeq")

            // 1. 사용자 건강 데이터 조회
            val stats = getUserHealthStats(userSeq).getOrElse {
                return Result.failure(it)
            }
            Log.d(tag, "[HEALTH_OTHER] 사용자 데이터: $stats")

            // 2. HealthData로 변환
            val healthData = convertStatsToHealthData(stats)

            // 3. 5초 로딩 시뮬레이션
            kotlinx.coroutines.delay(5000)

            // 4. 활동 수준 결정 및 더미 데이터 반환
            val activityLevel = determineActivityLevelForLetter(healthData)
            val dummySentences = AiLetterResponseValidator.getDummyLetter(
                groupType = AiLetterResponseValidator.GroupType.CARE,
                activityLevel = activityLevel
            )
            Log.d(tag, "[HEALTH_OTHER] 더미 데이터 반환: $dummySentences")

            Result.success(dummySentences)
        } catch (e: Exception) {
            Log.e(tag, "[HEALTH_OTHER] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ④ wellness-other: 헬스 그룹 멤버 격려 메시지 (3문장)
     * - 특정 사용자의 운동 데이터 + 헬스 그룹 목표값 포함
     * - AI 사용하지 않고 5초 로딩 후 더미 데이터 반환
     * - UI에 표시용 (서버 저장 안 함)
     */
    suspend fun generateHealthGroupMemberEncouragement(
        userSeq: Long,
        groupSeq: Long
    ): Result<List<String>> {
        return try {
            Log.d(tag, "[WELLNESS_OTHER] ④ 헬스 멤버 격려 메시지 생성: userSeq=$userSeq, groupSeq=$groupSeq")

            // 1. 사용자 운동 데이터 조회
            val stats = getUserHealthStats(userSeq).getOrElse {
                return Result.failure(it)
            }
            Log.d(tag, "[WELLNESS_OTHER] 사용자 데이터: $stats")

            // 2. 개인 일일 목표 조회
            val dailyGoal = getDailyPersonalGoal(groupSeq)
            if (dailyGoal == null) {
                Log.w(tag, "[WELLNESS_OTHER] ⚠️ 그룹 목표 없음")
                return Result.failure(Exception("그룹 목표를 찾을 수 없습니다"))
            }

            // 3. HealthData로 변환 + 목표값 추가
            val healthData = convertStatsToHealthData(stats)
            val dataWithGoal = healthData.copy(
                bestMetric = dailyGoal.first,
                bestValue = dailyGoal.second
            )

            // 4. 5초 로딩 시뮬레이션
            kotlinx.coroutines.delay(5000)

            // 5. 활동 수준 결정 및 더미 데이터 반환
            val activityLevel = determineActivityLevelForLetter(dataWithGoal)
            val dummySentences = AiLetterResponseValidator.getDummyLetter(
                groupType = AiLetterResponseValidator.GroupType.HEALTH,
                activityLevel = activityLevel
            )
            Log.d(tag, "[WELLNESS_OTHER] 더미 데이터 반환: $dummySentences")

            Result.success(dummySentences)
        } catch (e: Exception) {
            Log.e(tag, "[WELLNESS_OTHER] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ⑤ health_other_short: 케어 그룹 빠른 넛지 메시지
     * - 짧은 응원 메시지 (20자 내외)
     * - health_other_short.gguf 모델 사용
     * - UI에 표시용 (서버 저장 안 함)
     */
    suspend fun generateQuickNudgeMessage(
        userSeq: Long
    ): Result<String> {
        return try {
            Log.d(tag, "[HEALTH_SHORT] ⑤ 빠른 넛지 메시지 생성: userSeq=$userSeq")

            // 1. 사용자 건강 데이터 조회
            val stats = getUserHealthStats(userSeq).getOrElse {
                return Result.failure(it)
            }

            // 2. HealthData로 변환
            val healthData = convertStatsToHealthData(stats)

            // 3. health_other_short 모델 로드
            val modelInfo = getModelInfo(ModelCategory.HEALTH, ModelPerspective.OTHER_SHORT)
                ?: return Result.failure(Exception("health_other_short 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }

            // 4. AI 코멘트 생성 (짧은 메시지 - 여러 문장)
            val comment = generateComment(healthData, modelInfo)
            Log.d(tag, "[HEALTH_SHORT] AI 생성: $comment")

            // 5. 그대로 반환 (ViewModel에서 문장 분리 및 검증 처리)
            Result.success(comment)
        } catch (e: Exception) {
            Log.e(tag, "[HEALTH_SHORT] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ⑥ wellness_other_short: 헬스 그룹 빠른 넛지 메시지
     * - 짧은 응원 메시지 (20자 내외)
     * - wellness_other_short.gguf 모델 사용
     * - UI에 표시용 (서버 저장 안 함)
     */
    suspend fun generateQuickWellnessNudgeMessage(
        userSeq: Long,
        groupSeq: Long
    ): Result<String> {
        return try {
            Log.d(tag, "[WELLNESS_SHORT] ⑥ 빠른 넛지 메시지 생성: userSeq=$userSeq, groupSeq=$groupSeq")

            // 1. 사용자 운동 데이터 조회
            val stats = getUserHealthStats(userSeq).getOrElse {
                return Result.failure(it)
            }

            // 2. 개인 일일 목표 조회
            val dailyGoal = getDailyPersonalGoal(groupSeq)
            if (dailyGoal == null) {
                Log.w(tag, "[WELLNESS_SHORT] ⚠️ 그룹 목표 없음")
                return Result.failure(Exception("그룹 목표를 찾을 수 없습니다"))
            }

            // 3. HealthData로 변환 + 목표값 추가
            val healthData = convertStatsToHealthData(stats)
            val dataWithGoal = healthData.copy(
                bestMetric = dailyGoal.first,
                bestValue = dailyGoal.second
            )

            // 4. wellness_other_short 모델 로드
            val modelInfo = getModelInfo(ModelCategory.WELLNESS, ModelPerspective.OTHER_SHORT)
                ?: return Result.failure(Exception("wellness_other_short 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }

            // 5. AI 코멘트 생성 (짧은 메시지 - 여러 문장)
            val comment = generateComment(dataWithGoal, modelInfo)
            Log.d(tag, "[WELLNESS_SHORT] AI 생성: $comment")

            // 6. 그대로 반환 (ViewModel에서 문장 분리 및 검증 처리)
            Result.success(comment)
        } catch (e: Exception) {
            Log.e(tag, "[WELLNESS_SHORT] ❌ 에러", e)
            Result.failure(e)
        }
    }

    // ========== Private Helper Functions ==========

    /**
     * 그룹의 개인 일일 목표 조회
     * @param groupSeq 헬스 그룹 ID
     * @return Pair(지표명, 목표값) 또는 null
     */
    private suspend fun getDailyPersonalGoal(groupSeq: Long): Pair<String, String>? {
        return try {
            // 1. 그룹 목표 조회
            val goalResponse = healthGroupRepository.getCurrentGoals(groupSeq).getOrNull()
            if (goalResponse == null) {
                Log.w(tag, "[GET_DAILY_GOAL] ⚠️ 그룹 목표 없음: groupSeq=$groupSeq")
                return null
            }

            // 2. 그룹 멤버수 조회
            val groupDetail = groupRepository.getGroupDetail(groupSeq)
            val memberCount = groupDetail.currentMembers.coerceAtLeast(1)

            // 3. selectedMetricType에 따른 개인 일일 목표 계산
            val (metricName, goalValue) = when (goalResponse.selectedMetricType ?: "STEPS") {
                "STEPS" -> {
                    val dailyGoal = (goalResponse.goalSteps / memberCount / 7).toInt()
                    "걸음수" to dailyGoal.toString()
                }
                "KCAL" -> {
                    val dailyGoal = (goalResponse.goalKcal / memberCount / 7).toInt()
                    "칼로리" to dailyGoal.toString()
                }
                "DURATION" -> {
                    val dailyGoal = (goalResponse.goalDuration / memberCount / 7).toInt()
                    "운동시간" to dailyGoal.toString()
                }
                "DISTANCE" -> {
                    val dailyGoal = (goalResponse.goalDistance / memberCount / 7)
                    "거리" to String.format("%.1f", dailyGoal)
                }
                else -> {
                    // 기본값: STEPS
                    val dailyGoal = (goalResponse.goalSteps / memberCount / 7).toInt()
                    "걸음수" to dailyGoal.toString()
                }
            }

            Log.d(tag, "[GET_DAILY_GOAL] ✅ groupSeq=$groupSeq, metric=$metricName, goal=$goalValue")
            Pair(metricName, goalValue)
        } catch (e: Exception) {
            Log.e(tag, "[GET_DAILY_GOAL] ❌ 에러: ${e.message}", e)
            null
        }
    }

    /**
     * 오늘 00:00부터 현재까지의 건강 데이터 수집
     */
    private suspend fun collectTodayHealthData(): HealthData {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val now = today.atTime(LocalTime.now())

        // 걸음수
        val stepData = stepReader.readToday()
        val steps = stepData.count.toInt()

        // 운동 데이터
        val exerciseData = exerciseReader.readToday()
        val totalDuration = (exerciseData.totalDuration / 60000).toInt() // 밀리초 → 분
        val totalDistance = exerciseData.exercises
            .flatMap { it.sessions ?: emptyList() }
            .sumOf { (it.distance ?: 0f).toDouble() } / 1000 // km 단위
        val totalCalories = exerciseData.totalCalories.toInt()

        // 심박수
        val heartRates = heartRateReader.readToday()
        val avgHeartRate = if (heartRates.isNotEmpty()) {
            heartRates.mapNotNull { it.heartRate }.average().toInt()
        } else 0

        return HealthData(
            steps = steps,
            duration = totalDuration,
            distance = totalDistance,
            kcal = totalCalories,
            heartRate = avgHeartRate,
            // Optional fields: 필요시 추가 구현
            sleepHours = null,
            waterMl = null,
            bloodPressure = null,
            bestMetric = null,
            bestValue = null
        )
    }

    /**
     * GroupDetailResponse의 ShareOptions에 따라 데이터 필터링
     * 케어 그룹용 - 선택 항목(수면, 물, 혈압, 혈당)만 필터링
     */
    private fun filterHealthDataByPermissions(
        data: HealthData,
        groupDetail: GroupDetailResponse
    ): HealthData {
        // 기본 필드는 항상 포함 (걸음수, 심박수, 운동)
        // 선택 필드는 그룹의 요구사항에 따라 포함
        // Note: GroupDetailResponse에 ShareOptions가 없으므로 일단 모든 데이터 포함
        // TODO: 실제 ShareOptions API 응답 구조 확인 필요
        return data
    }

    /**
     * 목표값 데이터 추가 (케어 그룹용)
     */
    private fun addGoalData(
        data: HealthData,
        goalCriteria: GoalCriteria?
    ): HealthData {
        if (goalCriteria == null) return data

        // 목표 중 가장 중요한 지표 선택
        val (bestMetric, bestValue) = selectBestMetric(data, goalCriteria)

        return data.copy(
            bestMetric = bestMetric,
            bestValue = bestValue
        )
    }

    /**
     * 가장 중요한 목표 지표 선택 (케어 그룹용)
     */
    private fun selectBestMetric(
        data: HealthData,
        goalCriteria: GoalCriteria
    ): Pair<String, String> {
        // 진척도 계산
        val stepProgress = if (goalCriteria.minStep > 0) {
            data.steps.toDouble() / goalCriteria.minStep
        } else 0.0

        val calorieProgress = if (goalCriteria.minCalorie > 0) {
            data.kcal / goalCriteria.minCalorie
        } else 0.0

        val durationProgress = if (goalCriteria.minDuration > 0) {
            data.duration.toDouble() / goalCriteria.minDuration
        } else 0.0

        val distanceProgress = if (goalCriteria.minDistance > 0) {
            data.distance / goalCriteria.minDistance
        } else 0.0

        // 가장 낮은 진척도의 지표를 선택 (개선이 필요한 영역)
        val metrics = listOf(
            Triple("걸음수", stepProgress, goalCriteria.minStep.toString()),
            Triple("칼로리", calorieProgress, goalCriteria.minCalorie.toString()),
            Triple("운동시간", durationProgress, "${goalCriteria.minDuration}"),
            Triple("거리", distanceProgress, String.format("%.1f", goalCriteria.minDistance))
        )

        val bestMetric = metrics.minByOrNull { it.second } ?: metrics[0]
        return Pair(bestMetric.first, bestMetric.third)
    }

    /**
     * AI 코멘트 생성
     */
    private suspend fun generateComment(
        healthData: HealthData,
        modelInfo: ModelInfo
    ): String {
        Log.d(tag, "[GENERATE] 코멘트 생성: model=${modelInfo.displayName}, data=$healthData")

        var finalResponse = ""

        sendMessageUseCase(healthData).collect { result ->
            when (result) {
                is SendMessageResult.Success -> {
                    finalResponse = result.finalResponse
                    Log.d(tag, "[GENERATE] ✅ 생성 완료 (원본): $finalResponse")
                }
                is SendMessageResult.Error -> {
                    throw Exception(result.message)
                }
                else -> {
                    // Streaming, Loading, PromptGenerated - skip
                }
            }
        }

        // 후처리: 연속된 이모티콘 제거
        val cleaned = postProcessResponse(finalResponse)
        Log.d(tag, "[GENERATE] 🧹 후처리 완료: $cleaned")

        return cleaned
    }

    /**
     * 모델 출력 후처리
     * - 연속된 이모티콘 제거 (💖 💖 💖... 같은 패턴)
     */
    private fun postProcessResponse(response: String): String {
        // 같은 이모티콘이 3개 이상 연속으로 나오는 패턴 제거
        // 예: "💖 💖 💖 💖" → ""
        val emojiPattern = Regex("""(\p{So})\s*\1\s*\1+""")
        return response.replace(emojiPattern, "").trim()
    }

    /**
     * 서버에 코멘트 저장
     */
    private suspend fun saveCommentToServer(
        groupSeq: Long,
        comment: String
    ): Result<Unit> {
        return try {
            // 숫자 없는 첫 번째 문장 추출 (서버 제한: 1~200자)
            val selectedSentence = selectBestSentence(comment)

            // 200자 제한
            val truncatedComment = if (selectedSentence.length > 200) {
                Log.w(tag, "[SAVE] ⚠️ 문장이 200자 초과: ${selectedSentence.length}자 → 200자로 자름")
                selectedSentence.take(200)
            } else {
                selectedSentence
            }

            Log.d(tag, "[SAVE] 원본: $comment")
            Log.d(tag, "[SAVE] 선택된 문장: $truncatedComment (${truncatedComment.length}자)")

            val request = AiCommentRequest(
                groupSeq = groupSeq,
                comment = truncatedComment
            )

            val response = aiApi.postAiComment(request)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(tag, "[SAVE] ✅ 저장 성공: groupSeq=$groupSeq")
                Result.success(Unit)
            } else {
                val errorMsg = "저장 실패: HTTP ${response.code()}"
                Log.e(tag, "[SAVE] ❌ $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(tag, "[SAVE] ❌ 예외", e)
            Result.failure(e)
        }
    }

    /**
     * 최적의 문장 선택
     * 1순위: 숫자가 없는 첫 번째 문장
     * 2순위: 숫자가 있는 첫 번째 문장
     */
    private fun selectBestSentence(text: String): String {
        val sentences = text.split("\n", ".", "!", "?")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) {
            return text.take(200)
        }

        // 숫자가 없는 문장 우선 선택
        val sentenceWithoutNumbers = sentences.firstOrNull { sentence ->
            !sentence.contains(Regex("\\d"))
        }

        return sentenceWithoutNumbers ?: sentences.first()
    }

    /**
     * 사용자 건강 통계 조회
     */
    private suspend fun getUserHealthStats(userSeq: Long): Result<UserHealthStatsResponse> {
        return try {
            val response = aiApi.getUserHealthStatsToday(userSeq)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("데이터 조회 실패: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * UserHealthStatsResponse를 HealthData로 변환
     */
    private fun convertStatsToHealthData(stats: UserHealthStatsResponse): HealthData {
        return HealthData(
            steps = stats.totalSteps,
            duration = stats.totalDuration,
            distance = stats.totalDistances / 1000.0, // m → km 변환
            kcal = stats.totalCalories.toInt(),
            heartRate = stats.avgHeartRates.toInt(),
            // sleepHours: 0보다 크고 유효한 값일 때만 포함
            sleepHours = if (stats.sleepDuration > 0) {
                (stats.sleepDuration / 60.0).takeIf { it > 0 }
            } else null,
            // waterMl: 0보다 크고 유효한 값일 때만 포함
            waterMl = if (stats.totalWaterIntakes > 0) {
                stats.totalWaterIntakes.toInt().takeIf { it > 0 }
            } else null,
            // bloodPressure: null이 아니고 "없음" 같은 텍스트가 없을 때만 포함
            bloodPressure = stats.lastBloodPressure?.takeIf {
                it.isNotBlank() &&
                !it.contains("없음", ignoreCase = true) &&
                !it.contains("null", ignoreCase = true) &&
                !it.contains("데이터", ignoreCase = true)
            }
        )
    }

    /**
     * 3문장 응답 파싱
     */
    private fun parseSentences(text: String, expectedCount: Int): List<String> {
        Log.d(tag, "[PARSE_SENTENCES] 입력: $text")

        // 1. ※ 기호나 번호 패턴으로 분리 (우선)
        // 예: "※ 1번 문장입니다 ※ 2번 문장입니다" → ["1번 문장입니다", "2번 문장입니다"]
        // 예: "1. 첫 번째 2. 두 번째 3. 세 번째" → ["첫 번째", "두 번째", "세 번째"]
        val markerPattern = Regex("""※\s*\d+번?\s+""")
        val numberPattern = Regex("""\d+\.\s+""")

        var sentences = when {
            markerPattern.containsMatchIn(text) -> {
                // ※ 기호로 분리
                text.split(markerPattern)
                    .map { it.replace("※", "").trim() }
                    .filter { it.isNotBlank() }
            }
            numberPattern.containsMatchIn(text) -> {
                // 숫자 번호로 분리
                text.split(numberPattern)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
            else -> {
                // 2. 기본 방식: 줄바꿈이나 마침표로 분리
                text.split("\n", ".", "!", "?")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
        }

        // 3. 각 문장에서 남은 ※ 제거 및 정리
        sentences = sentences.map { sentence ->
            sentence
                .replace("※", "")
                .replace(Regex("""\d+번?\s+"""), "")  // "1번", "2번" 등 제거
                .trim()
        }.filter { it.isNotBlank() }

        // 4. 기대한 개수만큼 반환
        val result = if (sentences.size >= expectedCount) {
            sentences.take(expectedCount)
        } else if (sentences.isNotEmpty()) {
            sentences
        } else {
            // 파싱 실패 시 전체를 단일 문장으로
            listOf(text.replace("※", "").trim())
        }

        Log.d(tag, "[PARSE_SENTENCES] 결과 (${result.size}개):")
        result.forEachIndexed { index, s ->
            Log.d(tag, "[PARSE_SENTENCES]   ${index + 1}. $s")
        }

        return result
    }

    /**
     * 건강 데이터를 기반으로 활동 수준 결정 (Self 모델용)
     * - 걸음수 5000 이상 또는 운동시간 30분 이상 → SUFFICIENT
     * - 그 외 → INSUFFICIENT
     */
    private fun determineActivityLevel(healthData: HealthData): AiResponseValidator.ActivityLevel {
        return when {
            healthData.steps >= 5000 || healthData.duration >= 30 -> {
                AiResponseValidator.ActivityLevel.SUFFICIENT
            }
            else -> {
                AiResponseValidator.ActivityLevel.INSUFFICIENT
            }
        }
    }

    /**
     * 건강 데이터를 기반으로 활동 수준 결정 (Letter 모델용)
     */
    private fun determineActivityLevelForLetter(healthData: HealthData): AiLetterResponseValidator.ActivityLevel {
        return when {
            healthData.steps >= 5000 || healthData.duration >= 30 -> {
                AiLetterResponseValidator.ActivityLevel.SUFFICIENT
            }
            else -> {
                AiLetterResponseValidator.ActivityLevel.INSUFFICIENT
            }
        }
    }

    /**
     * 건강 데이터를 기반으로 활동 수준 결정 (Nudge 모델용)
     */
    private fun determineActivityLevelForNudge(healthData: HealthData): AiNudgeResponseValidator.ActivityLevel {
        return when {
            healthData.steps >= 5000 || healthData.duration >= 30 -> {
                AiNudgeResponseValidator.ActivityLevel.SUFFICIENT
            }
            else -> {
                AiNudgeResponseValidator.ActivityLevel.INSUFFICIENT
            }
        }
    }

    /**
     * 모델 정보 조회
     */
    private fun getModelInfo(
        category: ModelCategory,
        perspective: ModelPerspective
    ): ModelInfo? {
        return ModelRegistry.models
            .firstOrNull { it.category == category && it.perspective == perspective }
            ?.let { ModelInfo.fromConfig(it) }
    }
}
