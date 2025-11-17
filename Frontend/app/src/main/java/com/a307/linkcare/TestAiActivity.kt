//package com.a307.linkcare
//
//import android.app.Application
//import android.llama.cpp.LLamaAndroid
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.a307.linkcare.common.theme.LinkCareTheme
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.launch
//import java.io.File
//
///**
// * AI 모델 테스트용 액티비티
// * llama 모듈이 제대로 동작하는지 간단히 확인
// */
//class TestAiActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            LinkCareTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    TestAiScreen()
//                }
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun TestAiScreen(viewModel: TestAiViewModel = viewModel()) {
//    val scrollState = rememberLazyListState()
//
//    // 자동 스크롤
//    LaunchedEffect(viewModel.logs.size) {
//        if (viewModel.logs.isNotEmpty()) {
//            scrollState.animateScrollToItem(viewModel.logs.size - 1)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("AI 모델 테스트") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            // 상태 표시
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(
//                    containerColor = if (viewModel.isModelLoaded) {
//                        MaterialTheme.colorScheme.primaryContainer
//                    } else {
//                        MaterialTheme.colorScheme.surfaceVariant
//                    }
//                )
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(
//                        text = "상태: ${if (viewModel.isModelLoaded) "✅ 모델 로드됨" else "⏸️ 대기 중"}",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//                    if (viewModel.isGenerating) {
//                        Text(
//                            text = "🤖 AI 생성 중...",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//            }
//
//            // 로그 목록
//            LazyColumn(
//                state = scrollState,
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(4.dp)
//            ) {
//                items(viewModel.logs) { log ->
//                    Text(
//                        text = log,
//                        style = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 2.dp)
//                    )
//                }
//            }
//
//            // 버튼들
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Button(
//                    onClick = { viewModel.testSystemInfo() },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = !viewModel.isGenerating
//                ) {
//                    Text("1️⃣ 시스템 정보 확인")
//                }
//
//                Button(
//                    onClick = { viewModel.checkModelFiles() },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = !viewModel.isGenerating
//                ) {
//                    Text("2️⃣ 모델 파일 확인")
//                }
//
//                Button(
//                    onClick = { viewModel.testLoadModel() },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = !viewModel.isModelLoaded && !viewModel.isGenerating
//                ) {
//                    Text("3️⃣ 모델 로드 테스트")
//                }
//
//                Button(
//                    onClick = { viewModel.testInference() },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = viewModel.isModelLoaded && !viewModel.isGenerating
//                ) {
//                    Text("4️⃣ 추론 테스트 (간단한 질문)")
//                }
//
//                OutlinedButton(
//                    onClick = { viewModel.clearLogs() },
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text("로그 초기화")
//                }
//            }
//        }
//    }
//}
//
///**
// * 테스트용 ViewModel
// */
//class TestAiViewModel(application: Application) : AndroidViewModel(application) {
//    private val tag = "TestAiViewModel"
//    private val llamaAndroid = LLamaAndroid.instance()
//
//    var logs by mutableStateOf(listOf<String>())
//        private set
//
//    var isModelLoaded by mutableStateOf(false)
//        private set
//
//    var isGenerating by mutableStateOf(false)
//        private set
//
//    private fun log(message: String) {
//        Log.d(tag, message)
//        logs = logs + message
//    }
//
//    fun clearLogs() {
//        logs = emptyList()
//    }
//
//    /**
//     * 1️⃣ 시스템 정보 확인
//     */
//    fun testSystemInfo() {
//        log("=== 시스템 정보 확인 ===")
//        try {
//            // LLamaAndroid가 제대로 로드되었는지 확인
//            log("✅ LLamaAndroid 인스턴스: ${llamaAndroid::class.simpleName}")
//            log("✅ llama 모듈 연결 성공!")
//            log("ℹ️ 네이티브 라이브러리가 정상적으로 로드되었습니다")
//        } catch (e: Exception) {
//            log("❌ 오류 발생: ${e.message}")
//            Log.e(tag, "testSystemInfo error", e)
//        }
//    }
//
//    /**
//     * 2️⃣ 모델 파일 확인
//     */
//    fun checkModelFiles() {
//        log("=== 모델 파일 확인 ===")
//        try {
//            val externalFilesDir = getApplication<Application>().getExternalFilesDir(null)
//            log("📁 경로: ${externalFilesDir?.absolutePath}")
//
//            val files = externalFilesDir?.listFiles()
//            if (files == null || files.isEmpty()) {
//                log("⚠️ 파일이 없습니다")
//                log("💡 ADB로 모델 파일을 전송해야 합니다:")
//                log("   adb push model.gguf /sdcard/Android/data/com.a307.linkcare/files/")
//            } else {
//                log("✅ 총 ${files.size}개 파일 발견:")
//                files.sortedBy { it.name }.forEach { file ->
//                    val sizeMB = file.length() / (1024 * 1024)
//                    log("   • ${file.name} (${sizeMB}MB)")
//                }
//            }
//        } catch (e: Exception) {
//            log("❌ 오류 발생: ${e.message}")
//            Log.e(tag, "checkModelFiles error", e)
//        }
//    }
//
//    /**
//     * 3️⃣ 모델 로드 테스트
//     */
//    fun testLoadModel() {
//        viewModelScope.launch {
//            log("=== 모델 로드 테스트 ===")
//            try {
//                val externalFilesDir = getApplication<Application>().getExternalFilesDir(null)
//
//                // .gguf 파일 찾기
//                val modelFile = externalFilesDir?.listFiles()?.firstOrNull {
//                    it.name.endsWith(".gguf")
//                }
//
//                if (modelFile == null) {
//                    log("❌ .gguf 모델 파일을 찾을 수 없습니다")
//                    log("💡 먼저 '2️⃣ 모델 파일 확인'을 실행하세요")
//                    return@launch
//                }
//
//                log("📦 모델 파일: ${modelFile.name}")
//                log("📊 크기: ${modelFile.length() / (1024 * 1024)}MB")
//                log("⏳ 로딩 중... (시간이 걸릴 수 있습니다)")
//
//                llamaAndroid.load(modelFile.absolutePath)
//
//                isModelLoaded = true
//                log("✅ 모델 로드 성공!")
//                log("🎉 이제 '4️⃣ 추론 테스트'를 실행할 수 있습니다")
//
//            } catch (e: Exception) {
//                log("❌ 모델 로드 실패: ${e.message}")
//                Log.e(tag, "testLoadModel error", e)
//                isModelLoaded = false
//            }
//        }
//    }
//
//    /**
//     * 4️⃣ 추론 테스트
//     */
//    fun testInference() {
//        viewModelScope.launch {
//            log("=== 추론 테스트 ===")
//            log("💬 질문: 안녕하세요?")
//            log("⏳ 응답 생성 중...")
//
//            isGenerating = true
//            var tokenCount = 0
//            var response = ""
//
//            try {
//                llamaAndroid.send("안녕하세요?", formatChat = false)
//                    .catch { e ->
//                        log("❌ Flow 오류: ${e.message}")
//                        isGenerating = false
//                    }
//                    .collect { token ->
//                        tokenCount++
//                        response += token
//
//                        // 처음 10개 토큰만 로그로 출력 (너무 많으면 화면이 느려짐)
//                        if (tokenCount <= 10) {
//                            log("🔤 토큰 #$tokenCount: '$token'")
//                        }
//
//                        // 간단한 종료 조건 (10개 토큰 또는 </s>)
//                        if (tokenCount >= 10 || token.contains("</s>")) {
//                            log("⏹️ 생성 중단 (테스트용)")
//                            return@collect
//                        }
//                    }
//
//                log("✅ 추론 완료!")
//                log("📝 생성된 토큰 수: $tokenCount")
//                log("📄 응답: $response")
//                log("🎉 llama 모듈이 정상적으로 동작합니다!")
//
//            } catch (e: Exception) {
//                log("❌ 추론 실패: ${e.message}")
//                Log.e(tag, "testInference error", e)
//            } finally {
//                isGenerating = false
//            }
//        }
//    }
//}
