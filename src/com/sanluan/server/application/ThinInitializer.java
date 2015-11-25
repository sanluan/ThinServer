package com.sanluan.server.application;

import java.util.Map;

import com.sanluan.server.Thin;
import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;

public interface ThinInitializer extends Thin {
    public void start(String webappPath, ThinHttpHandler handler);

    public Map<ThinServlet, String[]> registerServlet();

    public void stop();
}