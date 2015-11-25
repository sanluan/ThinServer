package com.sanluan.demo1;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.commons.logging.Log;

import com.sanluan.server.servlet.ThinServlet;
import com.sun.net.httpserver.HttpExchange;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateServlet implements ThinServlet {
    private Configuration config;
    final Log log = getLog(getClass());

    public TemplateServlet(Configuration config) {
        this.config = config;
    }

    @Override
    public void deal(String path, HttpExchange httpExchange) {
        try {
            Template template = config.getTemplate(path);
            httpExchange.sendResponseHeaders(200, 0);
            OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody(), DEFAULT_CHARSET);
            template.process(null, writer);
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
