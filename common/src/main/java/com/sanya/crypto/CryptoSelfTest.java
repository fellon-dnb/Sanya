package com.sanya.crypto;

import javax.crypto.SecretKey;
import java.security.*;
/**
 Само тест
 */
public final class CryptoSelfTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CryptoSelfTest start ===");

        // === Генерация ключей ===
        KeyPair aliceX = KeyUtils.generateX25519();
        KeyPair aliceS = KeyUtils.generateEd25519();
        KeyPair bobX   = KeyUtils.generateX25519();
        KeyPair bobS   = KeyUtils.generateEd25519();

        // === Подписываем bundle Алисы ===
        byte[] aXpubRaw = KeyUtils.x25519Raw((java.security.interfaces.XECPublicKey) aliceX.getPublic());
        byte[] toSign = Bytes.concat(Bytes.utf8("Alice"), aXpubRaw);
        byte[] signature = SignatureUtils.signEd25519(aliceS.getPrivate(), toSign);
        SignedPreKeyBundle bundle = new SignedPreKeyBundle("Alice", aXpubRaw, aliceS.getPublic().getEncoded(), signature, System.currentTimeMillis());

        // === Проверка подписи ===
        PublicKey aSignPub = KeyFactory.getInstance("Ed25519")
                .generatePublic(new java.security.spec.X509EncodedKeySpec(bundle.getEd25519Public()));
        boolean ok = SignatureUtils.verifyEd25519(aSignPub, toSign, bundle.getSignature());
        System.out.println("Signature verify: " + ok);
        if (!ok) throw new RuntimeException("Signature verification failed");

        // === ECDH + HKDF обе стороны ===
        PublicKey aXpub = KeyUtils.x25519FromRaw(bundle.getX25519Public());
        byte[] sharedBob = KeyUtils.sharedSecret(bobX.getPrivate(), aXpub);
        SecretKey keyBob = KeyUtils.deriveAesKey(sharedBob, null, Bytes.utf8("Sanya-Chat-AES"));

        byte[] sharedAlice = KeyUtils.sharedSecret(aliceX.getPrivate(), bobX.getPublic());
        SecretKey keyAlice = KeyUtils.deriveAesKey(sharedAlice, null, Bytes.utf8("Sanya-Chat-AES"));

        // === AES-GCM roundtrip ===
        String msg = "hello secure world";
        byte[] aad = Bytes.concat(Bytes.utf8("Alice"), Bytes.u64be(System.currentTimeMillis()));

        var box = AesGcm.encrypt(keyAlice, Bytes.utf8(msg), aad);
        byte[] plain = AesGcm.decrypt(keyBob, box.iv, box.ct, aad);
        String decoded = new String(plain);

        System.out.println("Decrypted: " + decoded);
        if (!msg.equals(decoded)) throw new RuntimeException("Roundtrip failed");

        System.out.println("OK — AES-GCM roundtrip successful");
        System.out.println("=== CryptoSelfTest done ===");
    }
}
