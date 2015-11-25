package com.sanluan.demo1;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sanluan.server.application.ThinInitializer;
import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;

import freemarker.template.Configuration;

public class TemplateInitializer implements ThinInitializer {
    Configuration config = new Configuration(Configuration.getVersion());

    @Override
    public void start(String webappPath, ThinHttpHandler handler) {
        try {
            config.setDirectoryForTemplateLoading(new File(webappPath));
        } catch (IOException e) {
        }
        config.setDefaultEncoding(DEFAULT_CHARSET.name());
    }

    @Override
    public Map<ThinServlet, String[]> registerServlet() {
        return new HashMap<ThinServlet, String[]>() {
            private static final long serialVersionUID = 1L;
            {
                put(new TemplateServlet(config), new String[] { "*.html" });
            }
        };
    }

    @Override
    public void stop() {
        config = null;
    }

}
