package com.tencent.bkrepo.common.security.crypto

import com.tencent.bkrepo.common.security.util.RsaUtils
import org.springframework.beans.factory.InitializingBean
import kotlin.reflect.KMutableProperty1

/**
 * 密钥归一化与校验。私钥统一转成单行 PKCS#8 Base64，公钥未配置时从私钥推导。
 * 代码里不再保留任何默认密钥，未配置直接启动失败。
 * helm 由 chart 渲染时生成，二进制部署用 scripts/gen-crypto-keys.sh 生成后填入配置。
 */
class CryptoPropertiesInitializer(
    private val properties: CryptoProperties
) : InitializingBean {

    override fun afterPropertiesSet() {
        RSA_FIELDS.forEach { (privateField, publicField) ->
            val privateKey = RsaUtils.normalizePrivateKey(required(privateField))
            privateField.set(properties, privateKey)
            val publicKey = clean(publicField.get(properties))
            publicField.set(properties, publicKey.ifEmpty { RsaUtils.derivePublicKey(privateKey) })
        }
        val aesKey = required(CryptoProperties::aesKey)
        check(aesKey.toByteArray().size in AES_KEY_LENGTHS) {
            "security.crypto.aesKey must be 16, 24 or 32 bytes"
        }
        val aesIv = required(CryptoProperties::aesIv)
        check(aesIv.toByteArray().size == AES_IV_LENGTH) {
            "security.crypto.aesIv must be $AES_IV_LENGTH bytes"
        }
        properties.aesKey = aesKey
        properties.aesIv = aesIv
    }

    private fun required(field: KMutableProperty1<CryptoProperties, String>): String {
        val value = clean(field.get(properties))
        check(value.isNotEmpty()) {
            "security.crypto.${field.name} is required, " +
                "generate it with scripts/gen-crypto-keys.sh"
        }
        return value
    }

    /**
     * 部署模板未被替换时会留下 __BK_REPO_XXX__ 或 ${BK_REPO_XXX}，一律按未配置处理，
     * 否则会带着占位符往下走，报错停在 Base64 解码上，运维看不懂
     */
    private fun clean(value: String): String {
        val trimmed = value.trim()
        val unresolved = trimmed.startsWith("\${") ||
            (trimmed.startsWith("__") && trimmed.endsWith("__"))
        return if (unresolved) "" else trimmed
    }

    companion object {
        private val AES_KEY_LENGTHS = setOf(16, 24, 32)
        private const val AES_IV_LENGTH = 16
        private val RSA_FIELDS = listOf(
            CryptoProperties::privateKeyStr to CryptoProperties::publicKeyStr,
            CryptoProperties::privateKeyStr2048PKCS8 to CryptoProperties::publicKeyStr2048PKCS8,
            CryptoProperties::privateKeyStr2048PKCS1 to CryptoProperties::publicKeyStr2048PKCS1
        )
    }
}
