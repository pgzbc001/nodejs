package com.mdm.platform.security;

import com.mdm.platform.common.BusinessException;
import com.mdm.platform.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 字段级加密服务（Q&A Q2 决议）：AES/ECB/PKCS5Padding 单密钥，密文 Base64 存储，展示脱敏。
 */
@Component
public class CryptoService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    /** 密文前缀，用于识别已加密值 */
    public static final String CIPHER_PREFIX = "ENC:";

    private final SecretKeySpec keySpec;

    public CryptoService(@Value("${app.security.aes-key}") String aesKey) {
        byte[] keyBytes = new byte[16];
        byte[] given = aesKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(given, 0, keyBytes, 0, Math.min(given.length, keyBytes.length));
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /** 加密（空值原样返回）。 */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "字段加密失败");
        }
    }

    /** 解密（非密文原样返回）。 */
    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(CIPHER_PREFIX)) {
            return stored;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(stored.substring(CIPHER_PREFIX.length()));
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "字段解密失败");
        }
    }

    /** 脱敏：保留前 4 后 4，中间以 **** 填充；长度不足 8 全掩码。 */
    public static String mask(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        if (plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
    }
}
