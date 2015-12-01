package com.sanluan.gpio4pi;

import java.io.IOException;

import com.sanluan.server.servlet.ThinServlet;
import com.sun.net.httpserver.HttpExchange;

public class ManagerServlet implements ThinServlet {
    public static final String PREFIX = "manager/";
    PiController controller;

    public ManagerServlet(PiController controller) {
        this.controller = controller;
    }

    @Override
    public void deal(String path, HttpExchange httpExchange) {
        String[] commonds = path.substring(PREFIX.length(), path.length()).split(SEPARATOR);
        if (null != commonds) {
            switch (commonds[0]) {
            case "click":
                if (2 == commonds.length) {
                    controller.click(commonds[1]);
                }
                break;
            }
            try {
                byte[] bytes = "{\"result\":\"success\"}".getBytes();
                httpExchange.sendResponseHeaders(200, bytes.length);
                httpExchange.getResponseBody().write(bytes);
                httpExchange.getResponseBody().flush();
            } catch (IOException e) {
            }
        }
    }
}
