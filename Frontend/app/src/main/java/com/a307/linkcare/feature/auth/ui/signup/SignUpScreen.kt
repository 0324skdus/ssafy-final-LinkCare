package com.a307.linkcare.feature.auth.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.component.atoms.LcInputField
import com.a307.linkcare.common.theme.*

@Composable
fun SignupScreen(
    onVerifyEmail: (String) -> Unit = {},
    onVerifyCode: (email: String, code: String, onResult: (Boolean) -> Unit) -> Unit,
    onSubmit: (name: String, email: String, password: String) -> Unit = { _, _, _ -> }
) {
    // --- 상태
    var name by rememberSaveable { mutableStateOf("") }                      // ✅ 이름
    var nameVerified by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordCheck by rememberSaveable { mutableStateOf("") }


    var emailLocked by rememberSaveable { mutableStateOf(false) }   // 인증 요청 후 이메일 입력 잠금
    var emailVerified by rememberSaveable { mutableStateOf(false) } // 최종 이메일 인증 완료
    var codeVerified by rememberSaveable { mutableStateOf(false) }  // 코드 검증 완료
    var isVerifying by rememberSaveable { mutableStateOf(false) }   // 코드 확인 로딩 상태

    val canConfirmName = name.length >= 2 && !nameVerified

    // --- 버튼/필드 활성 조건
    val canSendEmail = email.isNotBlank() && !emailLocked && !emailVerified
    val emailInputEnabled = !emailLocked && !emailVerified

    // 코드 확인: 이메일 잠김 상태 + 6자리 이상 + 미검증
    val canClickCode = emailLocked && !codeVerified && code.length >= 6

    // 최종 가입: 이메일 인증 완료 + 비번 규칙 + 비번 일치
    val isPwValid = password.length in 8..13 && hasMixedChars(password)
    val isPwSame = password.isNotBlank() && password == passwordCheck
    val canSubmit = nameVerified && emailVerified && isPwValid && isPwSame

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "당신과 함께하게 되어 기쁩니다",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            Text(
                text = "간단한 정보를 입력해주세요",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            Spacer(Modifier.height(23.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LcInputField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameVerified = false
                    },
                    placeholder = "이름을 입력하세요",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))

                val nameBtnText = if (nameVerified) "완료" else "확인"
                val nameBtnColor =
                    if (nameVerified) unActiveBtn else if (canConfirmName) main else unActiveBtn
                val nameBtnTextColor =
                    if (nameVerified) unActiveField else if (canConfirmName) white else unActiveField

                LcBtn(
                    text = nameBtnText,
                    modifier = Modifier.width(75.dp).height(46.dp),
                    buttonColor = nameBtnColor,
                    buttonTextColor = nameBtnTextColor,
                    isEnabled = canConfirmName,
                    onClick = {
                        if (canConfirmName) {
                            nameVerified = true
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // 이메일 인증
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LcInputField(
                    value = email,
                    onValueChange = {
                        email = it
                        // 이메일 바뀌면 인증 관련 상태 초기화
                        emailLocked = false
                        emailVerified = false
                        codeVerified = false
                        code = ""
                    },
                    placeholder = "이메일을 입력하세요",
                    modifier = Modifier.weight(1f),
                    enabled = emailInputEnabled
                )
                Spacer(Modifier.width(8.dp))

                val emailBtnText = when {
                    emailVerified -> "완료"
                    emailLocked -> "취소"
                    else -> "인증"
                }
                val emailBtnEnabled = when {
                    emailVerified -> false
                    emailLocked -> true
                    else -> canSendEmail
                }
                val emailBtnColor = when {
                    emailVerified -> unActiveBtn
                    emailLocked -> white
                    canSendEmail -> main
                    else -> unActiveBtn
                }
                val emailBtnTextColor = when {
                    emailVerified -> unActiveField
                    emailLocked -> main
                    canSendEmail -> white
                    else -> unActiveField
                }

                LcBtn(
                    text = emailBtnText,
                    modifier = Modifier.width(75.dp).height(46.dp),
                    buttonColor = emailBtnColor,
                    buttonTextColor = emailBtnTextColor,
                    isEnabled = emailBtnEnabled,
                    onClick = {
                        when {
                            // 취소: 이메일 다시 입력 가능, 검증 리셋
                            emailLocked && !emailVerified -> {
                                emailLocked = false
                                code = ""
                                codeVerified = false
                            }
                            // 인증: 이메일 잠그고 서버에 발송 요청
                            !emailLocked && !emailVerified && canSendEmail -> {
                                onVerifyEmail(email)
                                emailLocked = true
                                code = ""
                                codeVerified = false
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // 이메일 인증 확인
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LcInputField(
                    value = code,
                    onValueChange = {
                        code = it
                        codeVerified = false
                    },
                    placeholder = "인증번호를 입력하세요",
                    modifier = Modifier.weight(1f),
                    enabled = emailLocked && !emailVerified
                )
                Spacer(Modifier.width(8.dp))

                val codeBtnText = if (codeVerified) "완료" else "확인"
                val codeBtnEnabled = if (codeVerified) false else canClickCode

                val codeBtnColor = when {
                    codeVerified -> unActiveBtn
                    canClickCode -> main
                    else -> unActiveBtn
                }
                val codeBtnTextColor = when {
                    codeVerified -> unActiveField
                    canClickCode -> white
                    else -> unActiveField
                }

                LcBtn(
                    text = codeBtnText,
                    modifier = Modifier.width(75.dp).height(46.dp),
                    buttonColor = codeBtnColor,
                    buttonTextColor = codeBtnTextColor,
                    isEnabled = codeBtnEnabled,
                    onClick = {
                        if (!codeVerified && canClickCode) {
                            onVerifyCode(email, code) { success ->
                                if (success) {
                                    codeVerified = true
                                    emailVerified = true
                                } else {
                                    // 실패면 그대로 '확인' 상태 유지
                                }
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // 비밀번호
            LcInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력하세요",
                isPassword = true
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = "* 영문, 숫자, 특수문자를 조합하여 8~13자리를 입력해주세요",
                fontSize = 11.sp,
                color = unActiveField,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 비밀번호 확인
            LcInputField(
                value = passwordCheck,
                onValueChange = { passwordCheck = it },
                placeholder = "비밀번호 확인을 입력하세요",
                isPassword = true
            )

            Spacer(Modifier.height(20.dp))

            // 회원가입 완료 버튼
            LcBtn(
                text = "가입하기",
                modifier = Modifier.fillMaxWidth(),
                buttonColor = if (canSubmit) main else unActiveBtn,
                buttonTextColor = if (canSubmit) white else unActiveField,
                isEnabled = canSubmit,
                onClick = { onSubmit(name, email, password) }
            )
        }
    }
}

private fun hasMixedChars(pw: String): Boolean {
    val hasLetter = pw.any { it.isLetter() }
    val hasDigit = pw.any { it.isDigit() }
    val hasSpecial = pw.any { !it.isLetterOrDigit() }
    return hasLetter && hasDigit && hasSpecial
}
