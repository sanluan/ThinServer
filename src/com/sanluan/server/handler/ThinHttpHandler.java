package com.sanluan.server.handler;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;

import org.apache.commons.logging.Log;

import com.sanluan.server.Thin;
import com.sanluan.server.ThinHttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class ThinHttpHandler implements HttpHandler, Thin {
    protected String webappPath;
    protected ThinHttpServer httpServer;
    final Log log = getLog(getClass());

    abstract public ThinHttpHandler init();

    public String getWebappPath() {
        return webappPath;
    }

    public ThinHttpHandler setWebappPath(String webappPath) {
        this.webappPath = webappPath;
        return this;
    }

    public ThinHttpServer getHttpServer() {
        return httpServer;
    }

    public ThinHttpHandler setHttpServer(ThinHttpServer httpServer) {
        this.httpServer = httpServer;
        return this;
    }

    public String getPath(HttpExchange httpExchange) {
        String path = httpExchange.getRequestURI().getPath();
        if (!SEPARATOR.equals(path)) {
            path = path.substring(httpExchange.getHttpContext().getPath().length());
        }
        return path;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        execute(getPath(httpExchange), httpExchange);
    }

    abstract public void execute(String path, HttpExchange httpExchange);

    abstract public ThinHttpHandler shutdown();
}