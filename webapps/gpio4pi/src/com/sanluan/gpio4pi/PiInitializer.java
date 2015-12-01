package com.sanluan.gpio4pi;

import static com.sanluan.gpio4pi.ManagerServlet.PREFIX;

import java.util.HashMap;
import java.util.Map;

import com.sanluan.server.application.ThinInitializer;
import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;

public class PiInitializer implements ThinInitializer {
    PiController controller;

    @Override
    public Map<ThinServlet, String[]> registerServlet() {
        HashMap<ThinServlet, String[]> map = new HashMap<ThinServlet, String[]>();
        map.put(new ManagerServlet(controller), new String[] { SEPARATOR + PREFIX + "*" });
        return map;
    }

    @Override
    public void start(String webappPath, ThinHttpHandler handler) {
        this.controller = new PiController(handler);
    }

    @Override
    public void stop() {
        controller.shutdown();
    }
}