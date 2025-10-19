package com.sanya;

import com.sanya.crypto.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.*;
import java.util.HashMap;
import java.util.Map;

public final class ChatServerCryptoSelfTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== ChatServerCryptoSelfTest ===");

        // === 1. Запуск сервера в отдельном потоке ===
        Thread serverThread = new Thread(() -> {
            try {
                ChatServer server = new ChatServer();
                server.start();
            } catch (IOException e) {
                System.err.println("[Server] stopped: " + e.getMessage());
            }
        }, "ServerThread");
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(500); // подождать поднятие

        // === 2. Генерация ключей ===
        KeyPair ax = KeyUtils.generateX25519();
        KeyPair as = KeyUtils.generateEd25519();
        KeyPair bx = KeyUtils.generateX25519();
        KeyPair bs = KeyUtils.generateEd25519();

        // === 3. Подключаем Alice ===
        Socket sa = new Socket("localhost", 12345);
        sa.setSoTimeout(3000);
        ObjectOutputStream outA = new ObjectOutputStream(sa.getOutputStream());
        ObjectInputStream  inA  = new ObjectInputStream(sa.getInputStream());
        outA.writeObject(new Message("Alice", "<<<HELLO>>>"));
        outA.flush();

        byte[] axRaw = KeyUtils.x25519Raw((java.security.interfaces.XECPublicKey) ax.getPublic());
        byte[] sigA  = SignatureUtils.signEd25519(as.getPrivate(), Bytes.concat(Bytes.utf8("Alice"), axRaw));
        SignedPreKeyBundle aBundle = new SignedPreKeyBundle("Alice", axRaw,
                as.getPublic().getEncoded(), sigA, System.currentTimeMillis());
        outA.writeObject(aBundle);
        outA.flush();
        System.out.println("[Client A] Sent bundle");

        // === 4. Подключаем Bob ===
        Socket sb = new Socket("localhost", 12345);
        sb.setSoTimeout(3000);
        ObjectOutputStream outB = new ObjectOutputStream(sb.getOutputStream());
        ObjectInputStream  inB  = new ObjectInputStream(sb.getInputStream());
        outB.writeObject(new Message("Bob", "<<<HELLO>>>"));
        outB.flush();

        byte[] bxRaw = KeyUtils.x25519Raw((java.security.interfaces.XECPublicKey) bx.getPublic());
        byte[] sigB  = SignatureUtils.signEd25519(bs.getPrivate(), Bytes.concat(Bytes.utf8("Bob"), bxRaw));
        SignedPreKeyBundle bBundle = new SignedPreKeyBundle("Bob", bxRaw,
                bs.getPublic().getEncoded(), sigB, System.currentTimeMillis());
        outB.writeObject(bBundle);
        outB.flush();
        System.out.println("[Client B] Sent bundle");

        // === 5. Чтение входящих бандлов ===
        Map<String, SignedPreKeyBundle> receivedA = new HashMap<>();
        Map<String, SignedPreKeyBundle> receivedB = new HashMap<>();

        long end = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < end && (receivedA.isEmpty() || receivedB.isEmpty())) {
            try {
                Object oA = inA.readObject();
                if (oA instanceof SignedPreKeyBundle b) {
                    receivedA.put(b.getUsername(), b);
                } else if (oA instanceof Map<?, ?> map) {
                    map.forEach((k, v) -> {
                        if (v instanceof SignedPreKeyBundle sbun)
                            receivedA.put((String) k, sbun);
                    });
                }
            } catch (EOFException | SocketTimeoutException ignored) {}

            try {
                Object oB = inB.readObject();
                if (oB instanceof SignedPreKeyBundle b) {
                    receivedB.put(b.getUsername(), b);
                } else if (oB instanceof Map<?, ?> map) {
                    map.forEach((k, v) -> {
                        if (v instanceof SignedPreKeyBundle sbun)
                            receivedB.put((String) k, sbun);
                    });
                }
            } catch (EOFException | SocketTimeoutException ignored) {}
        }

        System.out.println("[Client A] got bundles: " + receivedA.keySet());
        System.out.println("[Client B] got bundles: " + receivedB.keySet());

        // === 6. Проверка подписей ===
        for (SignedPreKeyBundle bundle : receivedA.values()) {
            PublicKey pub = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(bundle.getEd25519Public()));
            boolean ok = SignatureUtils.verifyEd25519(
                    pub,
                    Bytes.concat(Bytes.utf8(bundle.getUsername()), bundle.getX25519Public()),
                    bundle.getSignature());
            if (!ok) throw new RuntimeException("Invalid signature from " + bundle.getUsername());
        }

        for (SignedPreKeyBundle bundle : receivedB.values()) {
            PublicKey pub = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(bundle.getEd25519Public()));
            boolean ok = SignatureUtils.verifyEd25519(
                    pub,
                    Bytes.concat(Bytes.utf8(bundle.getUsername()), bundle.getX25519Public()),
                    bundle.getSignature());
            if (!ok) throw new RuntimeException("Invalid signature from " + bundle.getUsername());
        }

        System.out.println("All signatures valid — bundles exchanged successfully");

        sa.close();
        sb.close();
        System.exit(0);
    }
}
