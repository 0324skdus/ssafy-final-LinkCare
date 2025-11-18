package com.a307.linkcare.feature.ai.domain.service

import android.app.Application
import android.util.Log
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
            Log.d(tag, "[CARE_SELF] ① 케어 그룹 코멘트 생성 시작")

            // 1. 모든 케어 그룹 조회
            val careGroups = groupRepository.getMyGroups("CARE")
            Log.d(tag, "[CARE_SELF] 케어 그룹 ${careGroups.size}개 발견")

            if (careGroups.isEmpty()) {
                Log.d(tag, "[CARE_SELF] 케어 그룹이 없어 종료")
                return Result.success(emptyList())
            }

            // 2. 오늘 00:00부터 현재까지의 건강 데이터 수집
            val todayHealthData = collectTodayHealthData()
            Log.d(tag, "[CARE_SELF] 오늘 건강 데이터: $todayHealthData")

            // 3. health_self 모델 로드
            val modelInfo = getModelInfo(ModelCategory.HEALTH, ModelPerspective.SELF)
                ?: return Result.failure(Exception("health_self 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }
            Log.d(tag, "[CARE_SELF] 모델 로드 완료: ${modelInfo.displayName}")

            val results = mutableListOf<String>()

            // 4. 각 그룹별로 코멘트 생성 및 저장
            for (group in careGroups) {
                Log.d(tag, "[CARE_SELF] 그룹 처리: ${group.groupName} (${group.groupSeq})")

                // 4-1. 그룹 상세 정보 조회 (ShareOptions 포함)
                val groupDetail = groupRepository.getGroupDetail(group.groupSeq)

                // 4-2. ShareOptions에 따라 데이터 필터링
                val filteredData = filterHealthDataByPermissions(
                    todayHealthData,
                    groupDetail
                )
                Log.d(tag, "[CARE_SELF] 필터링된 데이터: $filteredData")

                // 4-3. AI 코멘트 생성
                val comment = generateComment(filteredData, modelInfo)
                Log.d(tag, "[CARE_SELF] 생성된 코멘트: $comment")

                // 4-4. 서버에 저장
                val saveResult = saveCommentToServer(group.groupSeq, comment)
                if (saveResult.isSuccess) {
                    results.add("${group.groupName}: $comment")
                    Log.d(tag, "[CARE_SELF] ✅ 저장 성공: ${group.groupName}")
                } else {
                    Log.e(tag, "[CARE_SELF] ❌ 저장 실패: ${group.groupName}")
                }
            }

            Log.d(tag, "[CARE_SELF] ✅ 완료: ${results.size}/${careGroups.size}개 성공")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(tag, "[CARE_SELF] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ② wellness-self: 헬스 그룹용 본인 운동 상태 요약
     * - 모든 헬스 그룹에 대해 실행
     * - 각 그룹의 목표값 포함
     * - wellness_self.gguf 모델 사용
     * - POST /api/ai/comment
     */
    suspend fun generateAndSaveHealthGroupComments(): Result<List<String>> {
        return try {
            Log.d(tag, "[WELLNESS_SELF] ② 헬스 그룹 코멘트 생성 시작")

            // 1. 모든 헬스 그룹 조회
            val healthGroups = groupRepository.getMyGroups("HEALTH")
            Log.d(tag, "[WELLNESS_SELF] 헬스 그룹 ${healthGroups.size}개 발견")

            if (healthGroups.isEmpty()) {
                Log.d(tag, "[WELLNESS_SELF] 헬스 그룹이 없어 종료")
                return Result.success(emptyList())
            }

            // 2. 오늘 00:00부터 현재까지의 운동 데이터 수집
            val todayHealthData = collectTodayHealthData()
            Log.d(tag, "[WELLNESS_SELF] 오늘 운동 데이터: $todayHealthData")

            // 3. wellness_self 모델 로드
            val modelInfo = getModelInfo(ModelCategory.WELLNESS, ModelPerspective.SELF)
                ?: return Result.failure(Exception("wellness_self 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }
            Log.d(tag, "[WELLNESS_SELF] 모델 로드 완료: ${modelInfo.displayName}")

            val results = mutableListOf<String>()

            // 4. 각 그룹별로 코멘트 생성 및 저장
            for (group in healthGroups) {
                Log.d(tag, "[WELLNESS_SELF] 그룹 처리: ${group.groupName} (${group.groupSeq})")

                // 4-1. 그룹 상세 정보 조회 (목표값 포함)
                val groupDetail = groupRepository.getGroupDetail(group.groupSeq)

                // 4-2. 목표값 추가
                val dataWithGoal = addGoalData(todayHealthData, groupDetail.goalCriteria)
                Log.d(tag, "[WELLNESS_SELF] 목표값 포함 데이터: $dataWithGoal")

                // 4-3. AI 코멘트 생성
                val comment = generateComment(dataWithGoal, modelInfo)
                Log.d(tag, "[WELLNESS_SELF] 생성된 코멘트: $comment")

                // 4-4. 서버에 저장
                val saveResult = saveCommentToServer(group.groupSeq, comment)
                if (saveResult.isSuccess) {
                    results.add("${group.groupName}: $comment")
                    Log.d(tag, "[WELLNESS_SELF] ✅ 저장 성공: ${group.groupName}")
                } else {
                    Log.e(tag, "[WELLNESS_SELF] ❌ 저장 실패: ${group.groupName}")
                }
            }

            Log.d(tag, "[WELLNESS_SELF] ✅ 완료: ${results.size}/${healthGroups.size}개 성공")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(tag, "[WELLNESS_SELF] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ③ health-other: 케어 그룹 멤버 격려 메시지 (3문장)
     * - 특정 사용자의 건강 데이터 조회
     * - health_other.gguf 모델 사용
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

            // 3. health_other 모델 로드
            val modelInfo = getModelInfo(ModelCategory.HEALTH, ModelPerspective.OTHER)
                ?: return Result.failure(Exception("health_other 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }

            // 4. AI 코멘트 생성 (3문장)
            val comment = generateComment(healthData, modelInfo)
            Log.d(tag, "[HEALTH_OTHER] 생성된 격려 메시지: $comment")

            // 5. 3문장으로 분리
            val sentences = parseSentences(comment, expectedCount = 3)

            Result.success(sentences)
        } catch (e: Exception) {
            Log.e(tag, "[HEALTH_OTHER] ❌ 에러", e)
            Result.failure(e)
        }
    }

    /**
     * ④ wellness-other: 헬스 그룹 멤버 격려 메시지 (3문장)
     * - 특정 사용자의 운동 데이터 + 그룹 목표값 포함
     * - wellness_other.gguf 모델 사용
     * - UI에 표시용 (서버 저장 안 함)
     */
    suspend fun generateHealthGroupMemberEncouragement(
        userSeq: Long,
        goalCriteria: GoalCriteria?
    ): Result<List<String>> {
        return try {
            Log.d(tag, "[WELLNESS_OTHER] ④ 헬스 멤버 격려 메시지 생성: userSeq=$userSeq")

            // 1. 사용자 운동 데이터 조회
            val stats = getUserHealthStats(userSeq).getOrElse {
                return Result.failure(it)
            }
            Log.d(tag, "[WELLNESS_OTHER] 사용자 데이터: $stats")

            // 2. HealthData로 변환 + 목표값 추가
            val healthData = convertStatsToHealthData(stats)
            val dataWithGoal = addGoalData(healthData, goalCriteria)

            // 3. wellness_other 모델 로드
            val modelInfo = getModelInfo(ModelCategory.WELLNESS, ModelPerspective.OTHER)
                ?: return Result.failure(Exception("wellness_other 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }

            // 4. AI 코멘트 생성 (3문장)
            val comment = generateComment(dataWithGoal, modelInfo)
            Log.d(tag, "[WELLNESS_OTHER] 생성된 격려 메시지: $comment")

            // 5. 3문장으로 분리
            val sentences = parseSentences(comment, expectedCount = 3)

            Result.success(sentences)
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

            // 4. AI 코멘트 생성 (짧은 메시지)
            val comment = generateComment(healthData, modelInfo)
            Log.d(tag, "[HEALTH_SHORT] 생성된 넛지 메시지: $comment")

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
        goalCriteria: GoalCriteria?
    ): Result<String> {
        return try {
            Log.d(tag, "[WELLNESS_SHORT] ⑥ 빠른 넛지 메시지 생성: userSeq=$userSeq")

            // 1. 사용자 운동 데이터 조회
            val stats = getUserHealthStats(userSeq).getOrElse {
                return Result.failure(it)
            }

            // 2. HealthData로 변환 + 목표값 추가
            val healthData = convertStatsToHealthData(stats)
            val dataWithGoal = addGoalData(healthData, goalCriteria)

            // 3. wellness_other_short 모델 로드
            val modelInfo = getModelInfo(ModelCategory.WELLNESS, ModelPerspective.OTHER_SHORT)
                ?: return Result.failure(Exception("wellness_other_short 모델을 찾을 수 없습니다"))

            loadModelUseCase(modelInfo).getOrElse {
                return Result.failure(Exception("모델 로드 실패: ${it.message}"))
            }

            // 4. AI 코멘트 생성 (짧은 메시지)
            val comment = generateComment(dataWithGoal, modelInfo)
            Log.d(tag, "[WELLNESS_SHORT] 생성된 넛지 메시지: $comment")

            Result.success(comment)
        } catch (e: Exception) {
            Log.e(tag, "[WELLNESS_SHORT] ❌ 에러", e)
            Result.failure(e)
        }
    }

    // ========== Private Helper Functions ==========

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
        val totalDuration = (exerciseData.totalDuration / 60).toInt() // 분 단위
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
     * 목표값 데이터 추가
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
     * 가장 중요한 목표 지표 선택
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
        // 줄바꿈이나 마침표로 분리
        val sentences = text.split("\n", ".", "!", "?")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(expectedCount)

        return if (sentences.size == expectedCount) {
            sentences
        } else {
            // 기대한 개수가 안 나오면 전체를 단일 문장으로
            listOf(text.trim())
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
