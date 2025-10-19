package com.sanya.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
/**
 Утилиты работы с байтами
 */
public final class Bytes {
    private Bytes() {}

    public static byte[] concat(byte[]... arrs) {
        int len = 0;
        for (byte[] a : arrs) len += a.length;
        byte[] out = new byte[len];
        int p = 0;
        for (byte[] a : arrs) {
            System.arraycopy(a, 0, out, p, a.length);
            p += a.length;
        }
        return out;
    }

    public static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    public static byte[] u64be(long v) {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.putLong(v);
        return b.array();
    }

    public static byte[] copyOf(byte[] src) {
        return Arrays.copyOf(src, src.length);
    }

    public static void wipe(byte[] a) {
        if (a == null) return;
        Arrays.fill(a, (byte)0);
    }
}
