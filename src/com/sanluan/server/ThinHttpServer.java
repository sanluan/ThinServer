package com.sanluan.server;

import static com.sanluan.server.log.Log.getLog;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import com.sanluan.server.handler.DefaultHttpHandler;
import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.log.Log;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.spi.HttpServerProvider;

/**
 * main class
 * 
 * ThinHttpServer
 *
 */
public class ThinHttpServer implements Thin {
    public final static String SERVER_ROOT_PATH = "webapps";
    public final static String WEBAPP_ROOT_PATH = "ROOT";
    final Log log = getLog(getClass());
    private HttpServer httpserver;
    private ServerSocket serverSocket;
    private Map<String, ThinHttpHandler> handlerMap = new HashMap<String, ThinHttpHandler>();

    public ThinHttpServer(int port, int control_port) {
        try {
            httpserver = HttpServerProvider.provider().createHttpServer(new InetSocketAddress(port), 10000);
            httpserver.start();
            log.info("Http Listen on " + port);
            serverSocket = new ServerSocket(control_port);
            log.info("Control Listen on " + control_port);
            load(WEBAPP_ROOT_PATH);
            ThinServerController controller = new ThinServerController(this);
            controller.loadConfig();
            log.info("Http Listen on " + port);
            log.info("Control Listen on " + control_port);
            while (!serverSocket.isClosed()) {
                ThinServerController.createServer(serverSocket.accept(), controller);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ThinHttpServer(Integer.getInteger("com.sanluan.httpserver.MiniHttpServer.port", 80).intValue(), Integer.getInteger(
                "com.sanluan.httpserver.MiniHttpServer.controlPort", 8010).intValue());
    }

    public void stop() {
        stop(0);
    }

    public void stop(int i) {
        httpserver.stop(i);
    }

    public void load(String path) {
        load(path, null);
    }

    public void load(String path, String webappPath) {
        log.info("[" + path + "] initialize start!");
        ThinHttpHandler handler = new DefaultHttpHandler();
        if (WEBAPP_ROOT_PATH == path) {
            handler.setHttpServer(this);
        }
        if (null == webappPath) {
            webappPath = SERVER_ROOT_PATH + SEPARATOR + path;
        }
        File file = new File(webappPath);
        if (file.exists() && file.isDirectory()) {
            handlerMap.put(path, handler);
            httpserver.createContext(getContextPath(path), handler.setWebappPath(webappPath).init());
            log.info("[" + path + "] initialize complete!");
        } else {
            log.info("[" + path + "] not exists! path:" + webappPath);
        }
    }

    public void unLoad(String path) {
        handlerMap.get(path).shutdown();
        httpserver.removeContext(getContextPath(path));
        handlerMap.remove(path);
    }

    public void reLoad(String path) {
        unLoad(path);
        load(path, null);
    }

    public String getContextPath(String path) {
        if (WEBAPP_ROOT_PATH.equals(path)) {
            return SEPARATOR;
        } else {
            return SEPARATOR + path;
        }
    }

    public synchronized boolean shutdownServerSocketController() {
        try {
            if (null != serverSocket) {
                serverSocket.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Map<String, ThinHttpHandler> getHandlerMap() {
        return handlerMap;
    }
}