package app.api.diagnosticruntime.util;

import org.apache.logging.log4j.util.Strings;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {
    private static final String SECRET_KEY = "uRV8ShSpHkLlB6eD";

    public static String encrypt(String value) {
        if(Strings.isEmpty(value)) {
            return value;
        }
        try {
            SecretKey key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        if(Strings.isEmpty(encryptedValue)) {
            return encryptedValue;
        }
        try {
            SecretKey key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedValue);
            return new String(cipher.doFinal(decodedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting", e);
        }
    }

    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length % 16 == 0; // AES block size check
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
