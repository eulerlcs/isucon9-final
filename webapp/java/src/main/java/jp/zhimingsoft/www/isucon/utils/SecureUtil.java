package jp.zhimingsoft.www.isucon.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class SecureUtil {
    private static final Random RANDOM = new SecureRandom();
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    public static byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        RANDOM.nextBytes(salt);

        return salt;
    }

    public static byte[] getSecurePassword(String password, byte[] salt) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 100, 2048);

        // ハッシュ化
        byte[] challengePassword = null;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            SecretKey sk = skf.generateSecret(keySpec);
            challengePassword = sk.getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("error when encode password!", e);
        }

        return challengePassword;
    }
}
