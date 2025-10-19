package com.sanya.crypto;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.NamedParameterSpec;
import java.util.Base64;

public final class Crypto {
    // X25519 (JDK 17+)
    public static KeyPair genX25519() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("X25519");
            g.initialize(NamedParameterSpec.X25519);
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) { throw new RuntimeException(e); }
    }

    public static SecretKey deriveX25519(PrivateKey myPriv, PublicKey peerPub) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("X25519");
            ka.init(myPriv);
            ka.doPhase(peerPub, true);
            byte[] shared = ka.generateSecret();
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] key = sha256.digest(shared);              // KDF: SHA-256(shared)
            return new SecretKeySpec(key, 0, 32, "AES");     // AES-256
        } catch (GeneralSecurityException e) { throw new RuntimeException(e); }
    }

    public static byte[] randomNonce12() {
        byte[] n = new byte[12];
        new SecureRandom().nextBytes(n);
        return n;
    }

    public static byte[] encryptGCM(SecretKey key, byte[] nonce, byte[] plaintext) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return c.doFinal(plaintext);
        } catch (GeneralSecurityException e) { throw new RuntimeException(e); }
    }

    public static byte[] decryptGCM(SecretKey key, byte[] nonce, byte[] ciphertext) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            return c.doFinal(ciphertext);
        } catch (GeneralSecurityException e) { throw new RuntimeException(e); }
    }

    // сериализация публичных ключей X25519
    public static String encodePub(PublicKey k) { return Base64.getEncoder().encodeToString(k.getEncoded()); }
    public static PublicKey decodePub(String b64) {
        try {
            byte[] der = Base64.getDecoder().decode(b64);
            KeyFactory kf = KeyFactory.getInstance("X25519");
            return kf.generatePublic(new java.security.spec.X509EncodedKeySpec(der));
        } catch (GeneralSecurityException e) { throw new RuntimeException(e); }
    }
}
