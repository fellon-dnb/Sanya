package com.sanya.crypto;

import java.security.*;
/**
 подписи Ed25519
 */
public final class SignatureUtils {
    private SignatureUtils() {}

    public static byte[] signEd25519(PrivateKey priv, byte[] data) throws GeneralSecurityException {
        Signature s = Signature.getInstance("Ed25519");
        s.initSign(priv);
        s.update(data);
        return s.sign();
    }

    public static boolean verifyEd25519(PublicKey pub, byte[] data, byte[] sig) throws GeneralSecurityException {
        Signature s = Signature.getInstance("Ed25519");
        s.initVerify(pub);
        s.update(data);
        return s.verify(sig);
    }
}
