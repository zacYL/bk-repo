package net.devops.canway.common.lse.feign

import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CertTrustManager {

    private const val TLS = "TLS"
    private const val X509 = "X.509"

    val disableValidationTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    val trustAllHostname = HostnameVerifier { _, _ -> true }
    val disableValidationSSLSocketFactory = createSSLSocketFactory(disableValidationTrustManager)

    fun createSSLSocketFactory(certString: String): SSLSocketFactory {
        val trustManager = createTrustManager(certString)
        return createSSLSocketFactory(trustManager)
    }

    fun createSSLSocketFactory(trustManager: TrustManager): SSLSocketFactory {
        val sslContext = SSLContext.getInstance(TLS).apply { init(null, arrayOf(trustManager), null) }
        return sslContext.socketFactory
    }

    fun createTrustManager(certString: String): X509TrustManager {
        val certInputStream = certString.byteInputStream(Charsets.UTF_8)
        val certificateFactory = CertificateFactory.getInstance(X509)
        val certificateList = certificateFactory.generateCertificates(certInputStream)
        require(!certificateList.isEmpty()) { "Expected non-empty set of trusted certificates." }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        certificateList.forEachIndexed { index, certificate ->
            keyStore.setCertificateEntry(index.toString(), certificate)
        }
        val algorithm = TrustManagerFactory.getDefaultAlgorithm()
        val trustManagerFactory = TrustManagerFactory.getInstance(algorithm).apply { init(keyStore) }
        val trustManagers = trustManagerFactory.trustManagers
        check(trustManagers.size == 1) { "Unexpected default trust managers size: ${trustManagers.size}" }
        val firstTrustManager = trustManagers.first()
        check(firstTrustManager is X509TrustManager) { "Unexpected default trust managers:$firstTrustManager" }
        return firstTrustManager
    }
}
