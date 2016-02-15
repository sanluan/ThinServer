package com.sanluan.server.base;

import java.nio.charset.Charset;

/**
 * All classes should implements Thin
 * 
 * Thin
 *
 */
public interface Thin {
    public final static Charset DEFAULT_CHARSET = Charset.forName("utf-8");
    public static final String SEPARATOR = "/";
    public static final String BLANKSPACE = " ";
}
