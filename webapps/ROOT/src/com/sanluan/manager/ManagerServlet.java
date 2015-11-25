package com.sanluan.manager;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;

import org.apache.commons.logging.Log;

import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;
import com.sun.net.httpserver.HttpExchange;

public class ManagerServlet implements ThinServlet {
    public static final String PREFIX = "manager/";
    private ThinHttpHandler handler;
    final Log log = getLog(getClass());

    public ManagerServlet(ThinHttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void deal(String path, HttpExchange httpExchange) {
        String[] commonds = path.substring(PREFIX.length(), path.length()).split(SEPARATOR);
        log.info(path);
        for (String c : commonds) {
            log.info(c);
        }
        if (null != commonds) {
            switch (commonds[0]) {
            case "shutdown":
                handler.getHttpServer().stop();
                handler.getHttpServer().shutdownServerSocketController();
                break;
            case "load":
                if (2 < commonds.length) {
                    handler.getHttpServer().load(commonds[1], commonds[2]);
                } else {
                    handler.getHttpServer().load(commonds[1]);
                }
                break;
            case "unload":
                handler.getHttpServer().unLoad(commonds[1]);
                break;
            case "reload":
                handler.getHttpServer().reLoad(commonds[1]);
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
