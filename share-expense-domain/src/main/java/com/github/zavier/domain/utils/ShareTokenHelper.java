package com.github.zavier.domain.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 分享相关加解密
 *
 */
public class ShareTokenHelper {

    private static final String SHARE_AES_KEY = "dRrZacqq]Ay>s0kL%zyE@m5hbLZz!12N";

    private static final String SHARE_HMAC_KEY = "Y#k9TPRjo0c0s]0uXyxAdNA5LXzfXnqj]_#EpDuzF~q!T*%umg+0jQ00@Bd7?5PX";
    
    public static String generateShareToken(String payload) {
        // 先对消息体进行AES加密, 返回base64编码数据
        final String base64Body = aesEncrypt(payload);
        // 生成签名
        final String signature = generateSignatureForBase64(base64Body);
        // 返回token
        return base64Body + "." + signature;
    }

    public static boolean validateShareToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return false;
        }
        String base64Body = parts[0];
        String signature = parts[1];
        String expectedSignature = generateSignatureForBase64(base64Body);
        return expectedSignature.equals(signature);
    }

    public static String getShareTokenBody(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid token");
        }
        return aesDecrypt(parts[0]);
    }

    private static String generateSignatureForBase64(String base64Payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SHARE_HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(base64Payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    private static String aesEncrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SHARE_AES_KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getUrlEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while encrypting", e);
        }
    }

    private static String aesDecrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SHARE_AES_KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while decrypting", e);
        }
    }


}
