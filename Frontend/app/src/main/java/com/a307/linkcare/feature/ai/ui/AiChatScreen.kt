//package com.a307.linkcare.feature.ai.ui
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//
///**
// * AI 채팅 화면
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AiChatScreen(
//    viewModel: AiChatViewModel = hiltViewModel()
//) {
//    val uiState = viewModel.uiState
//    val scrollState = rememberLazyListState()
//    var expanded by remember { mutableStateOf(false) }
//
//    // 자동 스크롤
//    LaunchedEffect(uiState.messages.size) {
//        if (uiState.messages.isNotEmpty()) {
//            scrollState.animateScrollToItem(uiState.messages.size - 1)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("AI 상담") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            // 메시지 목록
//            LazyColumn(
//                state = scrollState,
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth(),
//                contentPadding = PaddingValues(16.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(uiState.messages) { message ->
//                    MessageItem(message = message)
//                }
//            }
//
//            HorizontalDivider()
//
//            // 입력 영역
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                // 모델 선택 드롭다운
//                ExposedDropdownMenuBox(
//                    expanded = expanded,
//                    onExpandedChange = { expanded = !expanded && !uiState.isGenerating }
//                ) {
//                    OutlinedTextField(
//                        value = viewModel.availableModels[uiState.selectedModelIndex].displayName,
//                        onValueChange = {},
//                        readOnly = true,
//                        label = { Text("모델 선택") },
//                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .menuAnchor(),
//                        enabled = !uiState.isGenerating,
//                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
//                    )
//                    ExposedDropdownMenu(
//                        expanded = expanded,
//                        onDismissRequest = { expanded = false }
//                    ) {
//                        viewModel.availableModels.forEachIndexed { index, modelInfo ->
//                            DropdownMenuItem(
//                                text = {
//                                    Column {
//                                        Text(modelInfo.displayName)
//                                        Text(
//                                            text = "타입: ${modelInfo.type}",
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                                        )
//                                    }
//                                },
//                                onClick = {
//                                    viewModel.selectModel(index)
//                                    expanded = false
//                                },
//                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
//                            )
//                        }
//                    }
//                }
//
//                // 메시지 입력
//                OutlinedTextField(
//                    value = uiState.userInput,
//                    onValueChange = { viewModel.updateUserInput(it) },
//                    modifier = Modifier.fillMaxWidth(),
//                    label = { Text("메시지 입력") },
//                    enabled = uiState.isModelLoaded && !uiState.isGenerating,
//                    maxLines = 3
//                )
//
//                // 버튼 Row 1
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    // 모델 로드
//                    Button(
//                        onClick = { viewModel.loadModel() },
//                        enabled = !uiState.isGenerating && !uiState.isLoading,
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text(
//                            if (uiState.isModelLoaded) {
//                                val currentModel = viewModel.availableModels[uiState.selectedModelIndex]
//                                if (uiState.currentLoadedModel?.filename == currentModel.filename) {
//                                    "로드됨"
//                                } else {
//                                    "모델 전환"
//                                }
//                            } else {
//                                "모델 로드"
//                            }
//                        )
//                    }
//
//                    // 전송
//                    Button(
//                        onClick = { viewModel.sendMessage() },
//                        enabled = uiState.isModelLoaded &&
//                                !uiState.isGenerating &&
//                                uiState.userInput.isNotBlank(),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text(if (uiState.isGenerating) "생성 중..." else "전송")
//                    }
//
//                    // 초기화
//                    OutlinedButton(
//                        onClick = { viewModel.clearMessages() },
//                        modifier = Modifier.weight(0.7f)
//                    ) {
//                        Text("초기화")
//                    }
//                }
//
//                // 버튼 Row 2
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    // 테스트 입력
//                    OutlinedButton(
//                        onClick = { viewModel.loadTestPrompt() },
//                        enabled = !uiState.isGenerating,
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text("테스트 입력")
//                    }
//                }
//            }
//        }
//    }
//}
//
///**
// * 메시지 아이템
// */
//@Composable
//fun MessageItem(message: com.a307.linkcare.feature.ai.domain.model.AiMessage) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = if (message.isUser) {
//                MaterialTheme.colorScheme.primaryContainer
//            } else {
//                MaterialTheme.colorScheme.secondaryContainer
//            }
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp)
//        ) {
//            Text(
//                text = if (message.isUser) "👤 사용자" else "🤖 AI",
//                style = MaterialTheme.typography.labelSmall,
//                color = if (message.isUser) {
//                    MaterialTheme.colorScheme.onPrimaryContainer
//                } else {
//                    MaterialTheme.colorScheme.onSecondaryContainer
//                }
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = message.content,
//                style = MaterialTheme.typography.bodyMedium,
//                color = if (message.isUser) {
//                    MaterialTheme.colorScheme.onPrimaryContainer
//                } else {
//                    MaterialTheme.colorScheme.onSecondaryContainer
//                }
//            )
//        }
//    }
//}
