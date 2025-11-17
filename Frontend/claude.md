# LinkCare Frontend 코드 구조 문서

## 📱 프로젝트 개요

### 기술 스택
- **플랫폼**: Native Android Application
- **언어**: Kotlin 100%
- **UI 프레임워크**: Jetpack Compose + Material3
- **아키텍처**: Clean Architecture
- **빌드 시스템**: Gradle (Kotlin DSL)
- **최소 SDK**: 29 (Android 10)
- **타겟 SDK**: 36

### 핵심 라이브러리
```
- Jetpack Compose: 선언형 UI
- Kotlin Coroutines: 비동기 처리
- Dagger Hilt: 의존성 주입
- Retrofit + OkHttp: REST API 통신
- DataStore: 토큰/설정 저장
- Navigation Compose: 타입 세이프 내비게이션
- Coil: 이미지 로딩
- Firebase: FCM, Analytics
- Google Sign-In: OAuth 인증
- Kakao SDK: 소셜 로그인
- Health Connect: 건강 데이터 연동
- Samsung Health Data API: 헬스 트래킹
```

### 앱 정보
- **패키지명**: `com.a307.linkcare`
- **앱명**: LinkCare
- **총 Kotlin 파일 수**: 111개

---

## 🏗️ 전체 디렉토리 구조

```
Frontend/
├── app/
│   ├── build.gradle.kts          # 앱 레벨 빌드 설정
│   ├── google-services.json       # Firebase 설정
│   ├── libs/                      # 로컬 AAR 라이브러리 (Samsung Health)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/            # 캐릭터 스프라이트 & 액세서리
│       │   ├── res/               # Android 리소스
│       │   └── java/com/a307/linkcare/
│       │       ├── MainActivity.kt
│       │       ├── LinkCareApp.kt
│       │       ├── common/        # 공통 컴포넌트 & 유틸
│       │       ├── feature/       # 기능 모듈
│       │       └── navigation/    # 내비게이션 설정
│       ├── androidTest/           # 계측 테스트
│       └── test/                  # 유닛 테스트
├── gradle/
│   └── libs.versions.toml         # 버전 카탈로그
├── build.gradle.kts               # 프로젝트 레벨 빌드 설정
└── settings.gradle.kts            # 프로젝트 설정
```

---

## 🎨 아키텍처 패턴

### Clean Architecture 레이어
프로젝트는 **Clean Architecture** 원칙을 따르며 명확한 계층 분리를 유지합니다:

1. **UI Layer** (`ui/` 폴더): Composable 스크린과 UI 로직
2. **Domain Layer** (`domain/` 폴더): 비즈니스 모델, 유스케이스
3. **Data Layer** (`data/` 폴더): API 인터페이스, 리포지토리, 데이터 소스

### 디자인 패턴
- **MVVM (Model-View-ViewModel)**: ViewModel이 UI 상태 관리
- **Repository Pattern**: 데이터 추상화 계층
- **Dependency Injection**: Hilt를 통한 의존성 관리
- **Atomic Design**: 컴포넌트 계층 구조 (atoms → molecules → pages)
- **Single Source of Truth**: StateFlow를 통한 반응형 상태 관리
- **Navigation Graph**: 중앙 집중식 내비게이션 로직

---

## 📂 주요 디렉토리 상세

### 루트 구조
```
linkcare/
├── MainActivity.kt              # 메인 액티비티 진입점
├── LinkCareApp.kt              # Application 클래스 (Hilt, Kakao 초기화)
├── common/                     # 기능 간 공유 리소스
│   ├── component/              # 재사용 가능한 UI 컴포넌트
│   │   ├── atoms/             # 기본 UI 요소
│   │   ├── molecules/         # 복합 컴포넌트
│   │   └── page/              # 페이지 레벨 컴포넌트
│   ├── di/                    # 의존성 주입 모듈
│   ├── theme/                 # 테마, 색상, 타이포그래피
│   └── util/                  # 유틸리티 함수
├── feature/                   # 기능 모듈 (수직 분할)
│   ├── auth/                  # 인증 & 온보딩
│   ├── caregroup/             # 케어 그룹 관리
│   ├── character/             # 캐릭터/아바타 시스템
│   ├── commongroup/           # 공유 그룹 기능
│   ├── healthgroup/           # 헬스 그룹 관리
│   ├── intro/                 # 앱 소개/투어
│   ├── mypage/                # 사용자 프로필 & 설정
│   └── notification/          # 알림 & 경고
└── navigation/                # 내비게이션 설정
    ├── AppNavGraph.kt         # 메인 내비게이션 그래프
    ├── Destinations.kt        # 라우트 정의
    └── MainTabs.kt           # 하단 탭 내비게이션
```

---

## 🔄 상태 관리

### ViewModel + StateFlow 패턴
- **패턴**: 단방향 데이터 플로우
- **상태 컨테이너**: `StateFlow<T>`를 통한 반응형 상태
- **생명주기 인지**: ViewModel은 설정 변경에도 유지됨
- **Hilt 통합**: `@HiltViewModel`로 의존성 주입

예시 구조:
```kotlin
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository
) : ViewModel() {
    private val _submit = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submit = _submit.asStateFlow()
}
```

### 토큰 관리
- **DataStore Preferences**: 영구 키-값 저장소
- **TokenStore**: 중앙 집중식 액세스/리프레시 토큰 관리
- **자동 갱신**: 401 처리를 위한 TokenAuthenticator

---

## 🧭 내비게이션 구조

### 3단계 내비게이션 계층

#### 1단계: 앱 레벨 라우트 (AppNavGraph.kt)
```
Route.Login         → 로그인 화면
Route.Signup        → 회원가입 플로우
Route.Intro         → 스플래시/인트로 애니메이션
Route.Onboarding    → 첫 사용자 온보딩 (페이저 기반)
Route.IntroTour     → 앱 기능 투어
Route.Main          → 메인 탭 컨테이너
```

#### 2단계: 탭 내비게이션 (MainTabs.kt)
4개의 메인 탭으로 구성된 하단 내비게이션:
```
Route.Care      → 케어 그룹 (의존성 추적)
Route.Health    → 헬스 그룹 (피트니스 챌린지)
Route.Alarm     → 알림 & 경고
Route.MyPage    → 사용자 프로필 & 설정
```

#### 3단계: 기능별 라우트
각 탭 내부의 중첩된 내비게이션 그래프:

**케어 탭**:
- `care/main` → 홈 화면 또는 빈 상태
- `care/search` → 그룹 검색
- `care/create_care_group` → 새 그룹 생성
- `care/edit_care_group/{groupId}` → 그룹 편집 (인자 포함)

**헬스 탭**:
- `health/main` → 홈 화면 또는 빈 상태
- `health/search` → 그룹 검색
- `health/create_health_group` → 새 그룹 생성
- `health/edit_health_group/{groupId}` → 그룹 편집 (인자 포함)

**마이페이지 탭**:
- `mypage/my_page_main` → 프로필 홈
- `mypage/my_groups` → 모든 사용자 그룹
- `mypage/decorate` → 아바타 커스터마이징
- `mypage/store` → 인앱 스토어
- `mypage/edit_profile` → 프로필 편집

### 내비게이션 구현 상세
- **타입 세이프 인자**: 라우트 파라미터용 NavArgument
- **Single Top**: 중복 화면 방지를 위한 실행 모드
- **PopUp 동작**: 설정 가능한 백스택 관리
- **상태 복원**: 탭 전환 시 자동 상태 복원

---

## 🌐 API/네트워킹 레이어

### 네트워크 모듈 (의존성 주입)
위치: `common/di/NetworkModule.kt`

**Base URL**: `http://k13a307.p.ssafy.io:9090/`

### Retrofit 설정
- **컨버터**: Gson + ScalarsConverter
- **로깅**: HttpLoggingInterceptor (BODY 레벨)
- **인증**: 커스텀 AuthInterceptor
- **토큰 갱신**: 자동 갱신을 위한 TokenAuthenticator

### API 인터페이스

#### AuthApi (인증)
```kotlin
POST /api/auth/google           # Google OAuth 로그인
POST /api/auth/kakao            # Kakao OAuth 로그인
POST /api/auth/login            # 이메일/비밀번호 로그인
POST /api/auth/signup           # 사용자 등록
POST /api/auth/refresh          # 토큰 갱신
GET  /api/auth/check-email      # 이메일 검증
POST /api/auth/send-verification-code
POST /api/auth/verify-code
```

#### ProfileApi
- 사용자 프로필 관리
- 온보딩 중 프로필 업데이트

#### CharacterApi
```kotlin
GET  /api/characters            # 모든 캐릭터 가져오기
GET  /api/characters/main       # 메인 캐릭터 가져오기
POST /api/characters/select     # 초기 캐릭터 선택
PUT  /api/characters/main       # 메인 캐릭터 설정
POST /api/characters/unlock     # 새 캐릭터 잠금 해제
```

#### CareGroupApi
- 케어 그룹 CRUD 작업

#### HealthGroupApi
- 헬스 그룹 CRUD 작업

### 인증 플로우
1. **로그인** → 액세스 + 리프레시 토큰 수신
2. **토큰 저장** DataStore에
3. **AuthInterceptor** 모든 요청에 `Authorization: Bearer {access}` 추가
4. **TokenAuthenticator** 401 에러 감지, 토큰 갱신, 요청 재시도
5. **로그아웃** → DataStore 클리어

---

## 🎯 기능 모듈 상세

모든 기능은 일관된 구조를 따릅니다: **data/ → domain/ → ui/**

### Feature: auth/
**목적**: 인증, 회원가입, 온보딩

**구조**:
```
auth/
├── data/
│   ├── AuthApi.kt                    # 로그인/회원가입 엔드포인트
│   ├── AuthInterceptor.kt            # 토큰 주입
│   ├── TokenAuthenticator.kt         # 토큰 갱신 로직
│   ├── TokenStore.kt                 # DataStore 래퍼
│   ├── ProfileApi.kt                 # 프로필 엔드포인트
│   ├── RetrofitClient.kt             # 싱글톤 retrofit 클라이언트
│   └── HealthPermissions.kt          # 건강 데이터 권한
├── domain/
│   ├── LoginRequest.kt               # 로그인 DTO
│   ├── LoginResponse.kt
│   ├── GoogleLoginRequest.kt
│   ├── KakaoLoginRequest.kt
│   ├── SignupRequest.kt
│   ├── OnboardingForm.kt             # 온보딩 상태
│   └── UpdateProfileRequest.kt
├── ui/
│   ├── LoginScreen.kt                # Google/Kakao/이메일 로그인
│   ├── SignupScreen.kt               # 이메일 회원가입
│   ├── IntroScreen.kt                # 스플래시 화면
│   ├── SignupGenderScreen.kt         # 온보딩 단계 1
│   ├── SignupBirthScreen.kt          # 온보딩 단계 2
│   ├── SignupHeightScreen.kt         # 온보딩 단계 3
│   └── SignupCharacterScreen.kt      # 온보딩 단계 4
├── OnboardingViewModel.kt            # 온보딩 상태 관리
├── HealthPermissionViewModel.kt      # 건강 권한
├── ProfileRepository.kt              # 프로필 데이터 레이어
└── ArePermissionsGrantedUseCase.kt   # 권한 로직
```

**플로우**: 로그인 → (첫 방문?) → 인트로 → 온보딩 (4단계) → 인트로 투어 → 메인

---

### Feature: character/
**목적**: 애니메이션이 있는 아바타/캐릭터 시스템

**구조**:
```
character/
├── data/
│   └── CharacterApi.kt               # 캐릭터 엔드포인트
├── domain/
│   ├── CharacterState.kt             # 캐릭터 상태 모델
│   ├── CharacterManifest.kt          # 캐릭터 메타데이터
│   └── CharacterStatusDto.kt         # API 응답 DTO
├── ui/
│   ├── CharacterScreen.kt            # 메인 캐릭터 표시
│   ├── SpriteAnimation.kt            # 스프라이트 애니메이션 로직
│   ├── LayeredSpriteAnimation.kt     # 레이어드 스프라이트
│   └── PreloadFrames.kt              # 이미지 프리로딩
├── CharacterRepository.kt            # 캐릭터 데이터 레이어
└── CharacterViewModel.kt             # 캐릭터 상태
```

**에셋**:
- 스프라이트는 `assets/sprites/bear/`와 `assets/sprites/duck/`에 저장
- 액세서리는 `assets/accessories/{character}/{type}/`에 저장
- 배치 데이터는 `assets/placements/`에 저장

---

### Feature: caregroup/
**목적**: 케어 그룹 관리 (의존성 추적)

**구조**:
```
caregroup/
├── data/
│   ├── CareGroupApi.kt               # 케어 그룹 엔드포인트
│   └── buildWeeklyChartData.kt       # 차트 데이터 변환
├── domain/
│   ├── CareWeekSummary.kt            # 주간 요약 모델
│   ├── MemberSeries.kt               # 멤버 데이터 시리즈
│   ├── ShareOptions.kt               # 프라이버시 설정
│   ├── Tap.kt                        # 탭 내비게이션 모델
│   └── WeeklyChartData.kt            # 차트 데이터 모델
├── CareGroupCreate.kt                # 그룹 생성 화면
├── CareGroupEdit.kt                  # 그룹 편집 화면
├── CareGroupHome.kt                  # 그룹 홈 화면
├── CareMemberDetail.kt               # 멤버 상세 화면
└── MemberHealthDialog.kt             # 건강 데이터 다이얼로그
```

**기능**: 그룹 생성, 멤버 관리, 건강 데이터 공유, 주간 요약

---

### Feature: healthgroup/
**목적**: 헬스 그룹 관리 (피트니스 챌린지)

**구조**:
```
healthgroup/
├── data/
│   └── HealthGroupApi.kt             # 헬스 그룹 엔드포인트
├── domain/
│   └── ShareOptions.kt               # 프라이버시 설정
├── HealthGroupCreate.kt              # 그룹 생성 화면
├── HealthGroupEdit.kt                # 그룹 편집 화면
├── HealthGroupHome.kt                # 그룹 홈 화면
└── HealthMemberDetail.kt             # 멤버 상세 화면
```

---

### Feature: commongroup/
**목적**: 공유 그룹 기능

**파일**:
```
commongroup/
├── domain/
│   ├── GroupItem.kt                  # 그룹 리스트 아이템 모델
│   └── Member.kt                     # 멤버 모델
├── GroupSearch.kt                    # 그룹 검색 화면
├── MainWithNoGroup.kt                # 빈 상태 화면
└── NudgeLetterCard.kt                # 넛지 레터 컴포넌트
```

---

### Feature: mypage/
**목적**: 사용자 프로필, 설정, 커스터마이징

**파일**:
```
mypage/
├── MyPage.kt                         # 프로필 홈 화면
├── MyGroups.kt                       # 모든 그룹 리스트
├── ProfileEditScreen.kt              # 프로필 편집
├── DecorateScreen.kt                 # 아바타 커스터마이징
└── StoreScreen.kt                    # 인앱 스토어
```

**기능**: 프로필 편집, 그룹 관리, 아바타 샵, 캐릭터 커스터마이징

---

### Feature: notification/
**목적**: 알림 및 경고

**구조**:
```
notification/
├── data/                             # 알림 API
├── Notification.kt                   # 메인 알림 화면
├── NotiList.kt                       # 알림 리스트
├── GroupList.kt                      # 그룹 알림
└── LetterList.kt                     # 레터 알림
```

**통합**: Firebase Cloud Messaging (FCM)

---

### Feature: intro/
**목적**: 앱 소개/온보딩 투어

**파일**:
```
intro/
└── IntroTourScreens.kt               # 기능 투어 화면
```

---

## 🧩 공통 컴포넌트 & 유틸리티

### 컴포넌트 계층 (Atomic Design)

#### Atoms (기본 UI 요소)
위치: `common/component/atoms/`

```
Avatar.kt                    # 사용자/캐릭터 아바타 표시
DatePagerRow.kt             # 날짜 내비게이션 행
GroupContributionBar.kt     # 그룹 기여도 시각화
LcBtn.kt                    # LinkCare 버튼 컴포넌트
LcInputField.kt             # LinkCare 입력 필드
SummaryBarRow.kt            # 요약 바 표시
SummaryChipList.kt          # 요약 칩 리스트
TapBanner.kt                # 탭 내비게이션이 있는 배너
ToggleRow.kt                # 토글 스위치 행
```

#### Molecules (복합 컴포넌트)
위치: `common/component/molecules/`

```
BottomBar.kt                # 하단 내비게이션 바
BaselineInputs.kt           # 기준선 입력 폼
GroupAvatarPicker.kt        # 그룹용 아바타 선택기
HeaderBack.kt               # 뒤로 가기 버튼이 있는 헤더
HeaderBasic.kt              # 기본 헤더
HeaderDropDown.kt           # 드롭다운 메뉴가 있는 헤더
HeaderSearch.kt             # 검색 필드가 있는 헤더
HeaderTitleOnly.kt          # 제목만 있는 헤더
HealthGroupHeaderCard.kt    # 헬스 그룹 헤더 카드
MemberRow.kt                # 멤버 리스트 아이템
MetricChartCard.kt          # 메트릭 차트 표시
PagerDots.kt                # 페이저 인디케이터 점
ProgressSummaryCard.kt      # 진행 상황 요약 카드
```

#### Page Components
위치: `common/component/page/`

```
ErrorScreen.kt              # 에러 상태 화면
```

---

### 테마 시스템
위치: `common/theme/`

```
Theme.kt                    # Material3 테마 설정
Color.kt                    # 색상 팔레트 정의
Type.kt                     # 타이포그래피 정의
```

**테마 상세**:
- Material3 디자인 시스템
- 라이트/다크 테마 지원
- 커스텀 색상 스킴 (main, point, white)
- 타이포그래피 시스템 정의

---

### 유틸리티
위치: `common/util/`

```
CroppedImageLoader.kt       # 이미지 크롭 유틸리티
```

---

### 의존성 주입 모듈
위치: `common/di/`

```
NetworkModule.kt            # Retrofit, OkHttp, API 인터페이스
HealthModule.kt             # HealthConnect 클라이언트
```

**Hilt 모듈**:
- `@Singleton` 스코프로 앱 전역 인스턴스
- 제공: Retrofit, OkHttpClient, API 인터페이스, TokenStore, HealthConnectClient

---

## 🚀 빌드 설정 및 진입점

### 진입점

#### Application 진입
```kotlin
// AndroidManifest.xml
<application android:name="com.a307.linkcare.LinkCareApp" ...>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
    </activity>
</application>
```

#### MainActivity.kt
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        setContent {
            LinkCareTheme {
                AppNavGraph()  // Navigation 진입점
            }
        }
    }
}
```

#### LinkCareApp.kt
```kotlin
@HiltAndroidApp
class LinkCareApp : Application() {
    override fun onCreate() {
        KakaoSdk.init(this, getString(R.string.kakao_native_app_key))
        printKeyHash()  // Kakao 콘솔 등록용
    }
}
```

---

### 빌드 설정

#### 루트 build.gradle.kts
```kotlin
plugins {
    android.application (8.13.1) apply false
    kotlin.android (2.0.21) apply false
    kotlin.compose (2.0.21) apply false
    google-services (4.4.0) apply false
    hilt.android (2.57.1) apply false
}
```

#### App build.gradle.kts
**주요 설정**:
- **Namespace**: `com.a307.linkcare`
- **Application ID**: `com.a307.linkcare`
- **Compile SDK**: 36
- **Min SDK**: 29
- **Target SDK**: 36
- **Java Version**: 11

**BuildConfig 필드**:
- `GOOGLE_WEB_CLIENT_ID`: local.properties에서
- `KAKAO_NATIVE_APP_KEY`: local.properties에서

**리소스 값**:
- `kakao_native_app_key`: 빌드 시 주입
- `kakao_scheme`: 앱 키로부터 생성
- `web_client_id`: Google OAuth 클라이언트 ID

**빌드 기능**:
- Jetpack Compose 활성화
- BuildConfig 활성화

---

### 주요 의존성

#### 핵심 의존성
```gradle
// Jetpack Compose
compose.bom (2024.09.00)
material3
navigation-compose (2.7.7)

// 의존성 주입
hilt-android (2.57.1)
hilt-navigation-compose (1.2.0)

// 네트워킹
retrofit (2.9.0)
gson (2.9.0)
okhttp3-logging (4.12.0)

// 저장소
datastore-preferences (1.1.1)

// 인증
play-services-auth (20.7.0)
kakao-sdk (2.15.0)

// Firebase
firebase-bom (34.5.0)
firebase-messaging
firebase-analytics

// 이미지 로딩
coil-compose (2.6.0)

// 코루틴
kotlinx-coroutines-android (1.7.3)

// 건강
health-connect-client (1.1.0-alpha07)
samsung-health-data-api (1.0.0)
```

---

### 권한 (AndroidManifest.xml)
```xml
<!-- 네트워크 -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- 건강 & 피트니스 -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
<uses-permission android:name="android.permission.BODY_SENSORS" />

<!-- 알림 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- 위치 (건강 기능용) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

### 리소스

#### Assets
```
assets/
├── accessories/              # 캐릭터 액세서리
│   ├── bear/
│   │   ├── glasses/
│   │   └── hat/
│   └── duck/
│       ├── glasses/
│       └── hat/
├── placements/               # 액세서리 배치 데이터
└── sprites/                  # 캐릭터 스프라이트 애니메이션
    ├── bear/
    │   ├── n1/ ~ n11/        # 일반 애니메이션
    │   └── v1/ ~ v3/         # 승리 애니메이션
    └── duck/
        ├── n1/ ~ n6/
        └── v1/ ~ v2/
```

#### Drawable 리소스
```
res/drawable/
├── background_1.png ~ background_8.png    # 아바타 배경
├── char_bear_1.png ~ char_bear_3.png      # 곰 캐릭터
├── char_duck_1.png ~ char_duck_2.png      # 오리 캐릭터
├── google_login.png                        # 로그인 에셋
├── coin.png                                # 인앱 화폐
└── header_*.png                            # UI 아이콘
```

---

## 📊 요약

이 프로젝트는 **최신 네이티브 Android 앱**으로 다음과 같이 구성되어 있습니다:

### 기술적 특징
- **Jetpack Compose**: 선언형 UI
- **Clean Architecture**: 기능 기반 모듈화
- **MVVM 패턴**: ViewModel + StateFlow
- **Dagger Hilt**: 의존성 주입
- **Retrofit + OkHttp**: REST API 통신
- **Atomic Design**: 컴포넌트 조직화
- **Type-safe Navigation**: Navigation Compose
- **DataStore**: 영구 데이터 저장
- **Firebase**: 푸시 알림
- **Health 통합**: Health Connect, Samsung Health
- **소셜 로그인**: Google, Kakao

### 코드 품질
- 111개의 Kotlin 파일
- 모든 기능에 걸쳐 일관된 아키텍처 패턴
- data, domain, UI 레이어 간 명확한 분리
- 유지보수 가능하고 확장 가능한 코드베이스

---

## 📝 추가 참고사항

### 개발 환경 설정
1. `local.properties` 파일에 다음 값 설정 필요:
   - `GOOGLE_WEB_CLIENT_ID`: Google OAuth 클라이언트 ID
   - `KAKAO_NATIVE_APP_KEY`: Kakao 네이티브 앱 키

2. Firebase 설정:
   - `google-services.json` 파일 필요 (이미 포함됨)

3. Samsung Health:
   - `libs/` 폴더에 AAR 라이브러리 포함됨

### 주요 플로우
1. **로그인 플로우**: 로그인 → (첫 방문 시) 인트로 → 온보딩 4단계 → 인트로 투어 → 메인
2. **그룹 생성**: 탭 선택 → 그룹 검색 또는 생성 → 그룹 홈
3. **캐릭터 커스터마이징**: 마이페이지 → 꾸미기/상점 → 아이템 구매/장착

---

**문서 생성일**: 2025-11-13
**프로젝트**: LinkCare Frontend (S13P31A307)
**브랜치**: Front_ai
