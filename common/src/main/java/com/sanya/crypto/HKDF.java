package com.sanya.crypto;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * RFC 5869 HMAC-SHA256 HKDF. Используется для вывода симметрических ключей (например AES-256) из ECDH-секретов.
 */
public final class HKDF {
    private HKDF() {}

    public static byte[] extract(byte[] salt, byte[] ikm) throws GeneralSecurityException {
        if (salt == null || salt.length == 0) {
            salt = new byte[32]; // zero-salt
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        return mac.doFinal(ikm);
    }

    public static byte[] expand(byte[] prk, byte[] info, int length) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        int hashLen = mac.getMacLength();
        int n = (int) Math.ceil((double) length / hashLen);
        if (n > 255) throw new IllegalArgumentException("too long");

        byte[] result = new byte[length];
        byte[] t = new byte[0];
        int pos = 0;
        for (int i = 1; i <= n; i++) {
            mac.reset();
            mac.update(t);
            if (info != null) mac.update(info);
            mac.update((byte) i);
            t = mac.doFinal();
            int copy = Math.min(t.length, length - pos);
            System.arraycopy(t, 0, result, pos, copy);
            pos += copy;
        }
        return result;
    }

    /** Полный цикл: получить 32-байтный AES-256-ключ из shared secret */
    public static byte[] deriveAesKey(byte[] sharedSecret, byte[] salt, String info) throws GeneralSecurityException {
        byte[] prk = extract(salt, sharedSecret);
        return expand(prk, info.getBytes(), 32);
    }
}