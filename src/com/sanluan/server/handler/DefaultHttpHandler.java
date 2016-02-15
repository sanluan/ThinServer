package com.sanluan.server.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sanluan.server.application.ThinInitializer;
import com.sanluan.server.servlet.DefaultServlet;
import com.sanluan.server.servlet.ThinServlet;
import com.sun.net.httpserver.HttpExchange;

public class DefaultHttpHandler extends ThinHttpHandler {
    private ThinClassLoader appClassLoader;
    private ThinHttpHandler customHandler;
    public final static String WEBAPP_INFO_PATH = SEPARATOR + "WEB-INF";
    public static final String WELCOME_FILE = "index.html";
    List<String> cachedUrl = new ArrayList<String>();
    Map<String, ThinServlet> cachedMappings = new LinkedHashMap<String, ThinServlet>();
    Map<String, ThinServlet> urlMappings = new LinkedHashMap<String, ThinServlet>();
    Map<String, ThinServlet> dirMappings = new LinkedHashMap<String, ThinServlet>();
    Map<String, ThinServlet> fileTypeMappings = new LinkedHashMap<String, ThinServlet>();
    ThinServlet defaultServlet;

    public ThinHttpHandler init() {
        appClassLoader = new ThinClassLoader(webappPath + WEBAPP_INFO_PATH);
        Class<ThinHttpHandler> handlerClass = appClassLoader.getHandler();
        if (null != handlerClass) {
            try {
                customHandler = handlerClass.newInstance().setWebappPath(webappPath).init();
            } catch (InstantiationException e) {
                log.error(e.getMessage());
            } catch (IllegalAccessException e) {
                log.error(e.getMessage());
            }
        }
        Map<ThinServlet, String[]> servletMappings = new LinkedHashMap<ThinServlet, String[]>();
        for (ThinInitializer initializer : appClassLoader.getInitializerList()) {
            initializer.start(webappPath, null == customHandler ? this : customHandler);
            servletMappings.putAll(initializer.registerServlet());
        }
        for (Entry<ThinServlet, String[]> entry : servletMappings.entrySet()) {
            if (null != entry.getValue()) {
                for (String path : entry.getValue()) {
                    ThinServlet servlet = entry.getKey();
                    if (null == defaultServlet && SEPARATOR.equals(path)) {
                        defaultServlet = servlet;
                    } else {
                        if (path.startsWith(SEPARATOR) && path.endsWith(SEPARATOR + "*")) {
                            dirMappings.put(path.substring(0, path.length() - 1), servlet);
                        } else if (path.startsWith("*.")) {
                            fileTypeMappings.put(path.substring(1), servlet);
                        } else {
                            urlMappings.put(path, servlet);
                        }
                    }
                    log.info(path + " mapping to " + servlet.getClass().getName());
                }
            }
        }
        if (null == defaultServlet) {
            defaultServlet = new DefaultServlet(webappPath);
        }
        return this;
    }

    @Override
    public ThinHttpHandler shutdown() {
        for (ThinInitializer initializer : appClassLoader.getInitializerList()) {
            initializer.stop();
        }
        if (null != customHandler) {
            customHandler.shutdown();
        }
        appClassLoader = null;
        return this;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (null != customHandler) {
            customHandler.handle(httpExchange);
        } else {
            super.handle(httpExchange);
        }
        httpExchange.close();
    }

    @Override
    public void execute(String path, HttpExchange httpExchange) {
        OutputStream output = httpExchange.getResponseBody();
        try {
            if (path.startsWith(WEBAPP_INFO_PATH)) {
                httpExchange.sendResponseHeaders(404, 0);
            } else {
                if (path.endsWith(SEPARATOR)) {
                    path += WELCOME_FILE;
                } else if ("".equals(path)) {
                    path = SEPARATOR + WELCOME_FILE;
                }
                getServlet(path).deal(path, httpExchange);
            }
            output.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
            try {
                output.close();
            } catch (IOException e1) {
            }
        }
    }

    protected void cache(String path, ThinServlet servlet) {
        while (50 <= cachedUrl.size()) {
            cachedMappings.remove(cachedUrl.remove(0));
        }
        cachedMappings.put(path, servlet);
        cachedUrl.add(path);
    }

    protected ThinServlet getServlet(String path) {
        ThinServlet servlet = cachedMappings.get(path);
        if (null == servlet) {
            servlet = urlMappings.get(path);
        }
        int sindex;
        String temp;
        if (null == servlet && 0 < (sindex = path.lastIndexOf(SEPARATOR))) {
            servlet = dirMappings.get((temp = path.substring(0, sindex + 1)));
            while (null == servlet && 0 < (sindex = temp.lastIndexOf(SEPARATOR, temp.length() - 2))) {
                servlet = dirMappings.get((temp = temp.substring(0, sindex + 1)));
            }
        }
        int pindex;
        if (null == servlet && 0 < (pindex = path.lastIndexOf("."))) {
            servlet = fileTypeMappings.get(path.substring(pindex, path.length()));
        }
        if (null == servlet) {
            servlet = defaultServlet;
        }
        cache(path, servlet);
        return servlet;
    }
}