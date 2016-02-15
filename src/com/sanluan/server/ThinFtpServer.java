package com.sanluan.server;

import static com.sanluan.server.log.Log.getLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import com.sanluan.server.base.ThinFtp;
import com.sanluan.server.handler.FtpHandler;
import com.sanluan.server.log.Log;

/**
 * main class
 * 
 * ThinHttpServer
 *
 */
public class ThinFtpServer implements Runnable, ThinFtp {
    final Log log = getLog(getClass());
    private ServerSocket serverSocket;
    private ServerSocket controllerSocket;
    private String rootPath;
    private Map<String, User> userMap = new HashMap<String, User>();

    public ThinFtpServer(String rootPath, int port, int control_port) {
        this.rootPath = rootPath;
        try {
            serverSocket = new ServerSocket(port);
            log.info("Ftp Listen on " + port);
            new Thread(this).start();
            controllerSocket = new ServerSocket(control_port);
            log.info("Control Listen on " + control_port);
            ThinFtpServerController controller = new ThinFtpServerController(this);
            controller.loadConfig();
            log.info("Ftp Listened on " + port);
            log.info("Control Listened on " + control_port);
            while (!controllerSocket.isClosed()) {
                ThinFtpServerController.createServer(controllerSocket.accept(), controller);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ThinFtpServer(System.getProperty("com.sanluan.server.ThinFtpServer.rootPath", ""), Integer.getInteger(
                "com.sanluan.server.ThinFtpServer.port", 21).intValue(), Integer.getInteger(
                "com.sanluan.server.ThinFtpServer.controlPort", 2121).intValue());
    }

    public void addUser(String name, String password, String path) {
        userMap.put(name, new User(name, password, null == path ? "" : path));
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public synchronized boolean shutdownControllerSocketServer() {
        try {
            if (null != controllerSocket) {
                controllerSocket.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                new Thread(new FtpHandler(serverSocket.accept(), rootPath, this)).start();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    public class User {
        public User(String name) {
            this.name = name;
        }

        public User(String name, String password, String path) {
            this.name = name;
            this.password = password;
            this.path = path;
        }

        private String name;
        private String password;
        private String path;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public User getUser(String username) {
        return userMap.get(username);
    }
}