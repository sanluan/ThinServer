package com.sanluan.server.handler;

import static com.sanluan.server.log.Log.getLog;

import java.io.IOException;

import com.sanluan.server.Thin;
import com.sanluan.server.ThinHttpServer;
import com.sanluan.server.log.Log;
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
        return httpExchange.getRequestURI().getPath().substring(httpExchange.getHttpContext().getPath().length());
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        execute(getPath(httpExchange), httpExchange);
    }

    abstract public void execute(String path, HttpExchange httpExchange);

    abstract public ThinHttpHandler shutdown();
}