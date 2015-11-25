package com.sanluan.manager;

import static com.sanluan.manager.ManagerServlet.PREFIX;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sanluan.server.application.ThinInitializer;
import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;

import freemarker.template.Configuration;

public class TemplateInitializer implements ThinInitializer {
    ThinHttpHandler handler;
    Configuration config = new Configuration(Configuration.getVersion());

    @Override
    public void start(String webappPath, ThinHttpHandler handler) {
        this.handler = handler;
        try {
            config.setDirectoryForTemplateLoading(new File(webappPath));
        } catch (IOException e) {
        }
        config.setDefaultEncoding(DEFAULT_CHARSET.name());
    }

    @Override
    public Map<ThinServlet, String[]> registerServlet() {
        HashMap<ThinServlet, String[]> map = new HashMap<ThinServlet, String[]>();
        map.put(new TemplateServlet(config, handler), new String[] { "*.html" });
        map.put(new ManagerServlet(handler), new String[] { SEPARATOR + PREFIX + "*" });
        return map;
    }

    @Override
    public void stop() {
        config = null;
    }

}
