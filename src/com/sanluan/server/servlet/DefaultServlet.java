package com.sanluan.server.servlet;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.sun.net.httpserver.HttpExchange;

public class DefaultServlet implements ThinServlet {
    private String webappPath;
    byte[] buffer = new byte[4096];
    final Log log = getLog(getClass());
    protected static final Map<String, String> CONTENT_TYPE = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put(".html", "text/html; charset=utf-8");
            put(".css", "text/css");
            put(".js", "text/javascript");
            put(".png", "image/png");
            put(".jpg", "image/jpeg");
            put(".gif", "image/gif");
        }
    };

    public DefaultServlet(String webappPath) {
        this.webappPath = webappPath;
    }

    private String getFileType(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."), fileName.length());
    }

    @Override
    public void deal(String path, HttpExchange httpExchange) {
        File file = new File(webappPath, path);
        try {
            if (file.exists() && !file.isHidden()) {
                if (file.isFile()) {
                    httpExchange.getResponseHeaders().add("Content-Type", CONTENT_TYPE.get(getFileType(file.getName())));
                    httpExchange.sendResponseHeaders(200, file.length());
                    FileInputStream fis = new FileInputStream(file);
                    OutputStream output = httpExchange.getResponseBody();
                    for (int n = 0; -1 != (n = fis.read(buffer));) {
                        output.write(buffer, 0, n);
                        output.flush();
                    }
                    fis.close();
                } else if (file.isDirectory()) {
                    httpExchange.getResponseHeaders().add("Location", path + SEPARATOR);
                    httpExchange.sendResponseHeaders(302, 0);
                }
            } else {
                httpExchange.sendResponseHeaders(404, 0);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
