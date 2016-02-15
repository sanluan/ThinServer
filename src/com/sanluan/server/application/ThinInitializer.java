package com.sanluan.server.application;

import java.util.Map;

import com.sanluan.server.base.ThinHttp;
import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.servlet.ThinServlet;

public interface ThinInitializer extends ThinHttp {
    public void start(String webappPath, ThinHttpHandler handler);

    public Map<ThinServlet, String[]> registerServlet();

    public void stop();
}