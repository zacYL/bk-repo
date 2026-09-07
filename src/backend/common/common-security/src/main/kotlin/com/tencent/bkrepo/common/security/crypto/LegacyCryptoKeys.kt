package com.tencent.bkrepo.common.security.crypto

/**
 * 升级前硬编码在 [CryptoProperties] 里的默认私钥，已随开源仓库公开，用它加密等同于没有加密。
 *
 * 只允许出现在 RsaUtils.decryptWithSource 的解密兜底分支，用于读取升级前写入的存量密文。
 * 任何加密路径引用它都是安全漏洞，不要放进 [CryptoProperties]，那样会被 Spring 绑定成活跃密钥。
 *
 * 存量密文由 LegacyCryptoRewriteJob 重加密完成后，本文件连同兜底分支一并删除。
 */
@Deprecated("仅用于解密升级前写入的存量数据，禁止用于加密")
object LegacyCryptoKeys {
    /**
     * 旧的 security.crypto.privateKeyStr，1024 位 PKCS#8。
     * 公钥不必保留，解密用不上；AES 与 2048 位密钥不做兼容。
     */
    const val RSA_1024_PRIVATE_KEY = """
        MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMaoDhrj+Da2tGpa
        wrE8et6vHBjprVj0UiCEza7JVymYTo9gd/pxNJRnbf6NehUL1WP8D6f5e2XZEDNf
        qXOqyEjPqOKtWIYI6ZLQeQIuAXgyGE5aP3/KVHFnxk+IuzcJtvqTAthfeuVXGel9
        ATP8hlEyDuCJe7/orBjIVYFk3p+PAgMBAAECgYAGYwLJFIk3YRpdzPszbYlZvXF+
        z4x2LqyxRPPD6c82lCH6dBSHZbpWBxk/NNc29AFxTHpIYTn5ZUgjDrFI+bWkqxvg
        qWS/oyfB6rxajIQjTeorsGvt/oumxQA7hvUE2XXLi218RXCURWgz/FZnvNhGhPYU
        OJWHoPeNlVx3V5mG8QJBAOzP9iSPcw1YJkv6uAgY4MRv1GqPu3NcMif+DQVPOZCN
        Pq7ynSg15Zl3HMpl6jAZNJ/AUXRby3tLhO8WiWr6C8cCQQDWwKbhy4AZ5SDigFIP
        tk/655Uzprm2JaZSvGkBeOSB9EYCUC1ApKeImrufPZpSj3Ood/fbMyA6cl8Bswl2
        z335AkBTNa+ToSQYKEUspWhM0BEKdRD6cI65NkgZbVc96lybwkWoS2+VVXrbtdLT
        +4OSawjmqTj13dtd82c+a3jVsg65AkEAn1kiO0caDZzj8s2OlpQL8rwmDMZ45Lw5
        FwkwzWPcAsWzsQG3IlFK8uUFtRoryXkiM+6Y3nCoSFYXQxaLPjqmWQJBAI2tn28X
        AHFcSd0UnS8L6exJuMdjCw4huI5FOeZ0arf5NrWDFoKU30Crw2ozmRBcDvtjDVH9
        sn8oLC2ObCFItlM=
    """
}
