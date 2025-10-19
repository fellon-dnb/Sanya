package com.sanya.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
/**
 AES-GCM шифрование/дешифрование
*/
public final class AesGcm {
    private static final SecureRandom RNG = new SecureRandom();

    public static final int IV_LEN = 12;     // 96 бит
    public static final int TAG_BITS = 128;  // 16 байт

    public static final class Box {
        public final byte[] iv;
        public final byte[] ct; // ciphertext || tag
        public Box(byte[] iv, byte[] ct) { this.iv = iv; this.ct = ct; }
    }

    public static Box encrypt(SecretKey key, byte[] plaintext, byte[] aad) throws Exception {
        byte[] iv = new byte[IV_LEN];
        RNG.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        if (aad != null) c.updateAAD(aad);
        byte[] ct = c.doFinal(plaintext);
        return new Box(iv, ct);
    }

    public static byte[] decrypt(SecretKey key, byte[] iv, byte[] ct, byte[] aad) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        if (aad != null) c.updateAAD(aad);
        return c.doFinal(ct);
    }
}