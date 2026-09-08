package com.tencent.bkrepo.common.security.http.oauth

import com.tencent.bkrepo.common.security.crypto.CryptoProperties
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.common.security.util.RsaUtils
import io.jsonwebtoken.SignatureAlgorithm
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import java.time.Duration

@DisplayName("Oauth 前缀认证吊销检查")
class OauthAuthHandlerTest {

    private val cryptoProperties = CryptoProperties()
    private val authenticationManager = mockk<AuthenticationManager>()
    private val handler = OauthAuthHandler(authenticationManager, cryptoProperties)

    @Test
    @DisplayName("库中已删除的 access token 不能通过 Oauth 前缀认证")
    fun `revoked access token is rejected even if jwt signature is valid`() {
        val token = signedAccessToken()
        every { authenticationManager.checkOauthToken(token) } throws AuthenticationException(
            "Access token check failed."
        )

        assertThrows<AuthenticationException> {
            handler.onAuthenticate(MockHttpServletRequest(), OauthAuthCredentials(token))
        }
    }

    @Test
    @DisplayName("库中存在的 access token 可以通过 Oauth 前缀认证")
    fun `stored access token authenticates`() {
        val token = signedAccessToken()
        every { authenticationManager.checkOauthToken(token) } returns USER_ID

        val userId = handler.onAuthenticate(MockHttpServletRequest(), OauthAuthCredentials(token))

        assertEquals(USER_ID, userId)
    }

    private fun signedAccessToken(): String {
        return JwtUtils.generateToken(
            signingKey = RsaUtils.stringToPrivateKey(cryptoProperties.privateKeyStr2048PKCS8),
            expireDuration = TOKEN_TTL,
            subject = USER_ID,
            claims = mapOf("scope" to listOf(SCOPE)),
            algorithm = SignatureAlgorithm.RS256
        )
    }

    companion object {
        private const val USER_ID = "user-1"
        private const val SCOPE = "PROJECT"
        private val TOKEN_TTL = Duration.ofHours(1)
    }
}
