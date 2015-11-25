package com.sanluan.server.servlet;

import com.sanluan.server.Thin;
import com.sun.net.httpserver.HttpExchange;

public interface ThinServlet extends Thin {
    public void deal(String path, HttpExchange httpExchange);
}