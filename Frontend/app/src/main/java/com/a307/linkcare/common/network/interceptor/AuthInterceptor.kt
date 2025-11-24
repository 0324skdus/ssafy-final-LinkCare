package com.a307.linkcare.common.network.interceptor

import android.content.Context
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.auth.data.model.response.LoginResponse
import com.google.gson.Gson
import okhttp3.*

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()

        val noAuth = req.url.encodedPath.contains("/api/auth/login") ||
                req.url.encodedPath.contains("/api/auth/refresh")

        if (noAuth) return chain.proceed(req)

        val access = tokenStore.getAccess().orEmpty()
        val newReq = if (access.isNotBlank()) {
            req.newBuilder()
                .addHeader("Authorization", "Bearer $access")
                .build()
        } else req

        return chain.proceed(newReq)
    }
}

class TokenAuthenticator(
    private val appContext: Context,
    private val baseUrl: HttpUrl
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 무한루프 방지: 이미 재시도한 요청이면 중단
        if (response.request.header("X-Retry") == "true") return null

        val store = TokenStore(appContext)
        val refresh = store.getRefresh().orEmpty()
        if (refresh.isBlank()) return null

        // 동기적으로 refresh 요청
        val client = OkHttpClient.Builder()
            .build()

        val refreshReq = Request.Builder()
            .url(baseUrl.newBuilder().addEncodedPathSegments("api/auth/refresh").build())
            .post(RequestBody.create(null, ByteArray(0))) // POST 바디 비워도 되는지 서버 스웨거 확인
            .addHeader("Authorization", "Bearer $refresh")
            .build()

        val refreshRes = try {
            client.newCall(refreshReq).execute()
        } catch (_: Exception) {
            return null
        }

        if (!refreshRes.isSuccessful) {
            // 401/404 등 → 재로그인 필요
            store.clear()
            return null
        }

        // 응답 바디 파싱 (문자열 아닌 JSON을 받으므로 Gson 필요)
        val bodyStr = refreshRes.body?.string().orEmpty()
        val auth = parseAuthResponse(bodyStr) ?: return null

        // 새 토큰 저장
        store.saveTokens(auth.accessToken, auth.refreshToken)

        // 원 요청을 새 access로 재작성
        val newAccess = auth.accessToken
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .header("X-Retry", "true")
            .build()
    }

    // 간단 파서 (Gson)
    private fun parseAuthResponse(json: String): LoginResponse? = try {
        Gson().fromJson(json, LoginResponse::class.java)
    } catch (_: Exception) { null }
}
