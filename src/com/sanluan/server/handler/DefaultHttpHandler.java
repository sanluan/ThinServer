package com.sanluan.server.handler;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;

import com.sanluan.server.Thin;
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
                    if (null == defaultServlet && SEPARATOR.equals(path)) {
                        defaultServlet = entry.getKey();
                    } else {
                        if (path.startsWith(SEPARATOR) && SEPARATOR.endsWith(SEPARATOR + "*")) {
                            dirMappings.put(path.substring(0, path.length() - 2), entry.getKey());
                        } else if (path.startsWith("*.")) {
                            fileTypeMappings.put(path.substring(1), entry.getKey());
                        } else {
                            urlMappings.put(path, entry.getKey());
                        }
                    }
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
        System.gc();
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
        if (50 <= cachedUrl.size()) {
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

class ThinClassLoader extends URLClassLoader implements Thin {
    final Log log = getLog(getClass());
    private static final String CLASS_EXT = ".class";
    private static final String WEBAPP_LIB = "/lib";
    private static final String WEBAPP_CLASSES = "/classes";
    private Class<ThinHttpHandler> handler;
    private List<Class<?>> classList = new ArrayList<Class<?>>();
    private List<ThinInitializer> initializerList = new ArrayList<ThinInitializer>();

    public ThinClassLoader(String webInfoPath) {
        super(getURLList(webInfoPath));
        loadLibarary(webInfoPath + WEBAPP_LIB);
    }

    private void loadLibarary(String libPath) {
        try {
            File library = new File(libPath);
            URL[] urls = getURLs().clone();
            if (library.exists() && library.isDirectory()) {
                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(libPath), new DirectoryStream.Filter<Path>() {
                    @Override
                    public boolean accept(Path entry) throws IOException {
                        return !Files.isDirectory(entry);
                    }
                });
                for (Path entry : stream) {
                    addURL(entry.toUri().toURL());
                }
            }
            for (URL url : urls) {
                try {
                    lookUpClasses(null, Paths.get(url.toURI()));
                } catch (URISyntaxException e) {
                    log.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static final URL[] getURLList(String webInfoPath) {
        File classes = new File(webInfoPath + WEBAPP_CLASSES);
        if (classes.exists() && classes.isDirectory()) {
            try {
                return new URL[] { classes.toURI().toURL() };
            } catch (MalformedURLException e) {
            }
        }
        return new URL[0];
    }

    private void lookUpClasses(String parentPath, Path path) {
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(path);
            for (Path entry : stream) {
                String fileName = entry.toFile().getName();
                String name = null == parentPath ? fileName : parentPath + "/" + fileName;
                if (Files.isDirectory(entry)) {
                    lookUpClasses(name, entry);
                } else if (fileName.endsWith(CLASS_EXT)) {
                    addClass(name);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void addClass(String className) {
        try {
            final Class<?> clazz = this.findClass(className.replace(SEPARATOR, ".")
                    .substring(0, className.lastIndexOf(CLASS_EXT)));
            if (null != clazz) {
                classList.add(clazz);
                if (Thin.class.isAssignableFrom(clazz)) {
                    if (ThinInitializer.class.isAssignableFrom(clazz)) {
                        try {
                            initializerList.add((ThinInitializer) clazz.newInstance());
                        } catch (InstantiationException | IllegalAccessException e) {
                            log.error("initializer class error:" + e.getMessage());
                        }
                    } else {
                        if (ThinHttpHandler.class.isAssignableFrom(clazz)) {
                            setHandler(clazz);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("load class error:" + e.getMessage());
        }
    }

    public List<Class<?>> getClassList() {
        return classList;
    }

    public List<ThinInitializer> getInitializerList() {
        return initializerList;
    }

    public Class<ThinHttpHandler> getHandler() {
        return handler;
    }

    @SuppressWarnings("unchecked")
    public void setHandler(Class<?> handler) {
        this.handler = (Class<ThinHttpHandler>) handler;
    }
}