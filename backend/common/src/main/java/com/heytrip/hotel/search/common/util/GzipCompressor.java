package com.heytrip.hotel.search.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP 压缩/解压工具
 */
public final class GzipCompressor {
    private GzipCompressor() {}

    /**
     * 压缩字节数组
     * @param input 原始字节数组
     * @return 压缩后的字节数组
     */
    public static byte[] compress(byte[] input) {
        if (input == null || input.length == 0) return new byte[0];
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(input);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("GZIP compress error", e);
        }
    }

    /**
     * 压缩字符串
     * @param input 原始字符串
     * @return 压缩后的字节数组
     */
    public static byte[] compressString(String input) {
        if (input == null) return new byte[0];
        return compress(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解压字节数组
     * @param compressed 压缩后的字节数组
     * @return 解压后的字节数组
     */
    public static byte[] decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return new byte[0];
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("GZIP decompress error", e);
        }
    }

    /**
     * 解压为字符串
     * @param compressed 压缩后的字节数组
     * @return 解压后的字符串
     */
    public static String decompressToString(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return "";
        return new String(decompress(compressed), StandardCharsets.UTF_8);
    }
}
