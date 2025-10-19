package com.sanya.crypto;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.XECPublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;
/**
 Генерация и преобразование ключей X25519/Ed25519
 */
public final class KeyUtils {
    private KeyUtils() {}

    // X25519 для ECDH (identity)
    public static KeyPair generateX25519() throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("XDH");
        kpg.initialize(new NamedParameterSpec("X25519"));
        return kpg.generateKeyPair();
    }

    // Ed25519 для подписей
    public static KeyPair generateEd25519() throws GeneralSecurityException {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    // ECDH → shared secret (сырой)
    public static byte[] sharedSecret(PrivateKey myPriv, PublicKey theirPub) throws GeneralSecurityException {
        KeyAgreement ka = KeyAgreement.getInstance("XDH");
        ka.init(myPriv);
        ka.doPhase(theirPub, true);
        return ka.generateSecret();
    }

    // HKDF → AES-256 ключ
    public static SecretKey deriveAesKey(byte[] shared, byte[] salt, byte[] info) throws GeneralSecurityException {
        byte[] k = HKDF.deriveAesKey(shared, salt, new String(info));
        return new SecretKeySpec(k, "AES");
    }

    // Кодирование/парсинг X25519 публичного ключа в 32 байта и обратно
    public static byte[] x25519Raw(XECPublicKey pub) {
        // u-координата в little endian недоступна напрямую. JCA даёт big integer (u).
        // Берём u как беззнаковый BigInteger и сериализуем в 32 байта big-endian.
        byte[] be = pub.getU().toByteArray();
        byte[] out = new byte[32];
        // скопировать be в out с конца
        int srcPos = Math.max(0, be.length - 32);
        int len = Math.min(32, be.length);
        System.arraycopy(be, srcPos, out, 32 - len, len);
        return out;
    }

    public static PublicKey x25519FromRaw(byte[] raw32) throws GeneralSecurityException {
        BigInteger u = new BigInteger(1, raw32);
        XECPublicKeySpec spec = new XECPublicKeySpec(new NamedParameterSpec("X25519"), u);
        return KeyFactory.getInstance("XDH").generatePublic(spec);
    }
}
