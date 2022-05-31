package com.tencent.bkrepo.auth.util

import cn.hutool.crypto.CryptoException
import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.asymmetric.RSA
import com.tencent.bkrepo.auth.constant.PRIVATE_KEY
import com.tencent.bkrepo.auth.constant.PUBLIC_KEY
import com.tencent.bkrepo.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RsaUtils(
    private val redisOperation: RedisOperation
){
    /**
     * 生成密钥对，并存入redis
     */
    fun generateRsa() {
        val rsa = RSA()
        redisOperation.set(PRIVATE_KEY, rsa.privateKeyBase64)
        redisOperation.set(PUBLIC_KEY, rsa.publicKeyBase64)
    }

    /**
     * 从redis获取公钥
     */
    fun getPublicKey(): String {
        if (redisOperation.get(PUBLIC_KEY) == null) {
            logger.info("public key is null,will to generate rsa")
            generateRsa()
        }
        return redisOperation.get(PUBLIC_KEY)!!
    }

    /**
     * 从redis获取私钥
     */
    fun getPrivateKey(): String {
//        if (redisOperation.get(PRIVATE_KEY) == null) {
//            logger.info("private key is null,will to generate rsa")
//            generateRsa()
//        }
        return redisOperation.get(PRIVATE_KEY)!!
    }

    /**
     * 公钥加密
     */
    fun encrypt(password: String, publicKey: String): String {
        val rsa = RSA(null, publicKey)
        return rsa.encryptBcd(password, KeyType.PublicKey)
    }

    /**
     * 私钥解密
     */
    fun decrypt(password: String): String {
        val privateKey = redisOperation.get(PRIVATE_KEY) ?: throw CryptoException("private key is null")
        val rsa = RSA(privateKey, null)
        return rsa.decryptStr(password, KeyType.PrivateKey)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RsaUtils::class.java)
    }
}
