package com.sanluan.server.log;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sanluan.server.base.Thin;

public class Log implements Thin {
    private String className;
    protected static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS zzz");
    PrintStream error = System.err;
    PrintStream out = System.out;

    Log(String className) {
        this.className = className;
    }

    public final void debug(Object message) {
        log(1, message);
    }

    public void info(Object message) {
        log(2, message);
    }

    public void warn(Object message) {
        log(3, message);
    }

    public final void error(Object message) {
        log(4, message);
    }

    protected void log(int type, Object message) {
        StringBuffer buf = new StringBuffer();
        synchronized (dateFormatter) {
            buf.append(dateFormatter.format(new Date()));
        }
        buf.append(" ");
        switch (type) {
        case 1:
            buf.append("[DEBUG] ");
            break;
        case 2:
            buf.append("[INFO] ");
            break;
        case 3:
            buf.append("[WARN] ");
            break;
        case 4:
            buf.append("[ERROR] ");
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (4 < stack.length) {
            buf.append(stack[3].toString());
        } else {
            buf.append(className);
            buf.append(" UNKNOWN METHOD");
        }
        buf.append("\r\n");
        buf.append(String.valueOf(message));
        write(type, buf);
    }

    protected void write(int type, StringBuffer buffer) {
        if (type > 3) {
            error.println(buffer.toString());
        } else {
            out.println(buffer.toString());
        }
    }

    public static Log getLog(Class<?> clazz) {
        return getLog(clazz.getName());
    }

    public static Log getLog(String className) {
        return new Log(className);
    }
}
