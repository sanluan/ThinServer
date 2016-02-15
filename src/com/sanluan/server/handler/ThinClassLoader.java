package com.sanluan.server.handler;

import static com.sanluan.server.log.Log.getLog;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.sanluan.server.application.ThinInitializer;
import com.sanluan.server.base.Thin;
import com.sanluan.server.base.ThinHttp;
import com.sanluan.server.log.Log;

public class ThinClassLoader extends URLClassLoader implements ThinHttp {
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

    public ThinClassLoader() throws MalformedURLException {
        super(new URL[] {});
    }

    private void loadLibarary(String libPath) {
        try {
            File library = new File(libPath);
            URL[] urls = getURLs().clone();
            if (library.exists() && library.isDirectory()) {
                Filter<Path> filter = new DirectoryStream.Filter<Path>() {
                    @Override
                    public boolean accept(Path entry) throws IOException {
                        return !Files.isDirectory(entry);
                    }
                };
                DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(libPath), filter);
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
            Class<?> clazz = this.loadClass(className.replace(SEPARATOR, ".").substring(0, className.lastIndexOf(CLASS_EXT)));
            if (null != clazz) {
                classList.add(clazz);
                if (Thin.class.isAssignableFrom(clazz)) {
                    if (ThinInitializer.class.isAssignableFrom(clazz)) {
                        try {
                            initializerList.add((ThinInitializer) clazz.newInstance());
                        } catch (InstantiationException | IllegalAccessException e) {
                            log.error("initializer class error:" + e.getMessage());
                        }
                    } else if (ThinHttpHandler.class.isAssignableFrom(clazz)) {
                        setHandler(clazz);
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