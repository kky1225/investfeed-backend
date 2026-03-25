package com.example.investfeed.common.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
@Converter
class AesEncryptor(
    @param:Value("\${security.encryption-key}")
    private val encryptionKey: String
) : AttributeConverter<String, String> {

    private val algorithm = "AES"

    private fun getKeySpec(): SecretKeySpec {
        val key = encryptionKey.padEnd(32, '0').substring(0, 32).toByteArray()
        return SecretKeySpec(key, algorithm)
    }

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, getKeySpec())
        return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.toByteArray()))
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, getKeySpec())
        return String(cipher.doFinal(Base64.getDecoder().decode(dbData)))
    }
}
