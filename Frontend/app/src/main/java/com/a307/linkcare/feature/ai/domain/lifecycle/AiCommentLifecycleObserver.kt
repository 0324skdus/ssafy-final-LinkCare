package com.a307.linkcare.feature.ai.domain.lifecycle

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.a307.linkcare.feature.ai.domain.worker.AiWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 생명주기 관찰자
 * - 앱 진입 시 AI 코멘트 생성 트리거
 */
@Singleton
class AiCommentLifecycleObserver @Inject constructor(
    private val aiWorkManager: AiWorkManager
) : DefaultLifecycleObserver {

    private val tag = "AiLifecycleObserver"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(tag, "[ON_START] 앱 시작 감지 - AI 코멘트 생성 트리거")

        scope.launch {
            // 즉시 AI 코멘트 생성 실행
            aiWorkManager.runNow()
        }
    }
}
