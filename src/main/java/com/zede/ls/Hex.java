package com.zede.ls;

/**
 *
 */
public class Hex {
    
    public static String toHexString(byte[] ab) {
        if (ab == null) {
            return "null";
        }
        return toHexString(ab, 0, ab.length);
    }

    public static String toHexString(byte[] ab, int istart, int iend) {
        if (iend > ab.length) {
            throw new IllegalArgumentException("array length " + ab.length + "<" + iend);
        }
        int len = iend - istart;
        StringBuilder sb = new StringBuilder(len * 2);
        toHexString(ab, istart, iend, sb);
        return sb.toString();
    }
    public static void toHexString(byte[] ab, int istart, int iend, StringBuilder sb) {
        if (ab.length < iend) {
            throw new java.lang.IllegalArgumentException(istart + " -> " + iend + " > " + ab.length);
        }
        for (int i = istart; i < iend; i++) {
            appendTo(sb, ab[i]);
        }
    }

    public static char[] acHex = "0123456789ABCDEF".toCharArray();

    public static StringBuilder appendTo(StringBuilder sb, byte b) {
        int b16 = b >>> 4;
        b16 &= 0x0F;
        sb.append(acHex[b16]);
        b16 = b & 0x0F;
        sb.append(acHex[b16]);
        return sb;
    }
    
}
