package com.sanluan.server;

import static com.sanluan.server.log.Log.getLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import com.sanluan.server.handler.ThinHttpHandler;
import com.sanluan.server.log.Log;
import com.sanluan.server.socket.ClientServerController;
import com.sanluan.server.socket.SocketServerController;

public class ThinServerController implements Thin {
    private ThinHttpServer httpserver;
    public final static String LOAD_CONF = "conf/load";
    public static final String COMMOND_LOAD = "load";
    public static final String COMMOND_RELOAD = "reload";
    public static final String COMMOND_BYE = "bye";
    public static final String COMMOND_UNLOAD = "unload";
    public static final String COMMOND_SHUTDOWN = "shutdown";
    public static final String COMMOND_GRANT = "grant";
    final Log log = getLog(getClass());

    public ThinServerController(ThinHttpServer httpServer) {
        this.httpserver = httpServer;
    }

    public static void main(String[] args) {
        final Log log = getLog(ThinServerController.class);
        ClientServerController client = new ClientServerController("localhost", Integer.getInteger(
                "com.sanluan.httpserver.MiniHttpServer.controlPort", 8010).intValue());
        if (null != args && 0 < args.length) {
            switch (args[0].toLowerCase()) {
            case COMMOND_SHUTDOWN:
                client.shutdown();
                break;
            case COMMOND_LOAD:
                switch (args.length) {
                case 2:
                    client.load(args[1]);
                    break;
                case 3:
                    client.load(args[1], args[2]);
                    break;
                default:
                    log.error("Invalid number of load parameters");
                }
                break;
            case COMMOND_UNLOAD:
                if (1 < args.length) {
                    client.unLoad(args[1]);
                } else {
                    log.error("Invalid number of load parameters");
                }
                break;
            case COMMOND_RELOAD:
                if (1 < args.length) {
                    client.reLoad(args[1]);
                } else {
                    log.error("Invalid number of load parameters");
                }
                break;
            case COMMOND_GRANT:
                if (1 < args.length) {
                    client.grant(args[1]);
                } else {
                    log.error("Invalid number of load parameters");
                }
                break;
            case COMMOND_BYE:
                client.close();
                break;
            }
        }
        try {
            Thread.sleep(100);
            client.close();
        } catch (InterruptedException e) {
        }
    }

    public synchronized boolean shutdownServerSocketController() {
        log.info("The server is shutting down!");
        return httpserver.shutdownServerSocketController();
    }

    public void loadConfig() {
        File loadConf = new File(LOAD_CONF);
        if (loadConf.exists() && loadConf.isFile()) {
            try {
                FileInputStream inputStream = new FileInputStream(loadConf);
                InputStreamReader read = new InputStreamReader(new FileInputStream(loadConf), DEFAULT_CHARSET);
                BufferedReader bufferedReader = new BufferedReader(read);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] paramters = line.split(BLANKSPACE);
                    switch (paramters.length) {
                    case 1:
                        load(paramters[0]);
                        break;
                    case 2:
                        if (COMMOND_GRANT.equalsIgnoreCase(paramters[0])) {
                            grant(paramters[1]);
                        } else {
                            load(paramters[0], paramters[1]);
                        }
                        break;
                    }
                }
                read.close();
                inputStream.close();
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    public synchronized void load(String path, String webappPath) {
        httpserver.load(path, webappPath);
    }

    public synchronized void reLoad(String path) {
        httpserver.reLoad(path);
    }

    public synchronized void grant(String path) {
        ThinHttpHandler handler = httpserver.getHandlerMap().get(path);
        if (null != handler) {
            handler.setHttpServer(httpserver);
        }
    }

    public synchronized void unLoad(String path) {
        httpserver.unLoad(path);
    }

    public synchronized void load(String path) {
        httpserver.load(path);
    }

    public synchronized void shutdown() {
        shutdownHttpserver();
        shutdownServerSocketController();
    }

    public synchronized void shutdownHttpserver() {
        log.info("The server is shutting down!");
        httpserver.stop();
        log.info("The server is already shutdown!");
    }

    public static void createServer(Socket socket, ThinServerController controller) {
        new Thread(new SocketServerController(socket, controller)).start();
    }
}