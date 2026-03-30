package com.example.investfeed.totp

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class TotpService {

    private val digits = 6
    private val period = 30L
    private val issuer = "investfeed"
    private val algorithm = "HmacSHA1"
    private val allowedDiscrepancy = 1
    private val secretLength = 20

    private val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val secureRandom = SecureRandom()

    fun generateSecret(): String {
        val bytes = ByteArray(secretLength)
        secureRandom.nextBytes(bytes)
        return base32Encode(bytes)
    }

    fun generateQrCodeBase64(secret: String, account: String): String {
        val uri = buildString {
            append("otpauth://totp/")
            append(URLEncoder.encode(issuer, "UTF-8"))
            append(":")
            append(URLEncoder.encode(account, "UTF-8"))
            append("?secret=").append(secret)
            append("&issuer=").append(URLEncoder.encode(issuer, "UTF-8"))
            append("&algorithm=SHA1")
            append("&digits=").append(digits)
            append("&period=").append(period)
        }

        val matrix = QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 200, 200)
        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream)
        val base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray())
        return "data:image/png;base64,$base64"
    }

    fun verifyCode(secret: String, code: String): Boolean {
        val currentTimeStep = System.currentTimeMillis() / 1000 / period
        for (i in -allowedDiscrepancy..allowedDiscrepancy) {
            if (generateCode(secret, currentTimeStep + i) == code) {
                return true
            }
        }
        return false
    }

    private fun generateCode(secret: String, timeStep: Long): String {
        val secretBytes = base32Decode(secret)
        val timeBytes = ByteArray(8)
        var value = timeStep
        for (i in 7 downTo 0) {
            timeBytes[i] = (value and 0xFF).toByte()
            value = value shr 8
        }

        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secretBytes, algorithm))
        val hash = mac.doFinal(timeBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = truncated % Math.pow(10.0, digits.toDouble()).toInt()
        return otp.toString().padStart(digits, '0')
    }

    private fun base32Encode(bytes: ByteArray): String {
        val result = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                result.append(base32Chars[(buffer shr bitsLeft) and 0x1F])
            }
        }
        if (bitsLeft > 0) {
            result.append(base32Chars[(buffer shl (5 - bitsLeft)) and 0x1F])
        }
        return result.toString()
    }

    private fun base32Decode(encoded: String): ByteArray {
        val result = ByteArrayOutputStream()
        var buffer = 0
        var bitsLeft = 0
        for (char in encoded.uppercase()) {
            val value = base32Chars.indexOf(char)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result.write((buffer shr bitsLeft) and 0xFF)
            }
        }
        return result.toByteArray()
    }
}
