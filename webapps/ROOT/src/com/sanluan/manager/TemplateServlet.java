package com.sanluan.manager;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;
import com.sun.net.httpserver.HttpExchange;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateServlet implements ThinServlet {
    private Configuration config;
    private ThinHttpHandler handler;
    final Log log = getLog(getClass());

    public TemplateServlet(Configuration config, ThinHttpHandler handler) {
        this.config = config;
        this.handler = handler;
    }

    @Override
    public void deal(String path, HttpExchange httpExchange) {
        try {
            Template template = config.getTemplate(path);
            httpExchange.sendResponseHeaders(200, 0);
            OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody(), DEFAULT_CHARSET);
            Map<String, Object> map = new HashMap<String, Object>();
            if (null != handler.getHttpServer()) {
                map.put("handlerMap", handler.getHttpServer().getHandlerMap());
            }
            template.process(map, writer);
        } catch (IOException e) {
            try {
                httpExchange.sendResponseHeaders(404, 0);
            } catch (IOException e1) {
            }
        } catch (TemplateException e) {
            log.info(e.getMessage());
            try {
                httpExchange.sendResponseHeaders(500, 0);
            } catch (IOException e1) {
            }
        }
    }
}
