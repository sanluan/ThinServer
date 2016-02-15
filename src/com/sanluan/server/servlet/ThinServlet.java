package com.sanluan.server.servlet;

import com.sanluan.server.base.ThinHttp;
import com.sun.net.httpserver.HttpExchange;

public interface ThinServlet extends ThinHttp {
    public void deal(String path, HttpExchange httpExchange);
}