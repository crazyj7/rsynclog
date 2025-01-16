import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public class CryptoUtil {
    // 입력받은 키를 해시하여 16바이트 키로 변환
    private static byte[] generateKey(String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        // AES-128을 위해 16바이트만 사용
        return Arrays.copyOf(hash, 16);
    }

    public static String encrypt(String value, String key) throws Exception {
        byte[] keyBytes = generateKey(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        // UTF-8로 명시적 인코딩
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = cipher.doFinal(valueBytes);
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encrypted, String key) throws Exception {
        byte[] keyBytes = generateKey(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        // UTF-8로 명시적 디코딩
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
} 