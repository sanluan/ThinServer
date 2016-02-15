package com.sanluan.server;

import static com.sanluan.server.log.Log.getLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import com.sanluan.server.base.ThinFtp;
import com.sanluan.server.log.Log;
import com.sanluan.server.socket.FtpServerControllerSocketClient;
import com.sanluan.server.socket.FtpServerControllerSocketServer;

public class ThinFtpServerController implements ThinFtp {
    private ThinFtpServer ftpserver;
    public final static String FTP_CONF = "conf/ftp.conf";
    public static final String COMMOND_ADDUSER = "adduser";
    public static final String COMMOND_BYE = "bye";
    public static final String COMMOND_SHUTDOWN = "shutdown";
    final Log log = getLog(getClass());

    public ThinFtpServerController(ThinFtpServer ftpserver) {
        this.ftpserver = ftpserver;
    }

    public static void main(String[] args) {
        final Log log = getLog(ThinFtpServerController.class);
        FtpServerControllerSocketClient client = new FtpServerControllerSocketClient("localhost", Integer.getInteger(
                "com.sanluan.server.ThinHttpServer.controlPort", 2121).intValue());
        if (null != args && 0 < args.length) {
            switch (args[0].toLowerCase()) {
            case COMMOND_SHUTDOWN:
                client.shutdown();
                break;
            case COMMOND_ADDUSER:
                if (1 < args.length) {
                    client.adduser(args[1]);
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

    public void loadConfig() {
        File file = new File(FTP_CONF);
        if (file.exists() && file.isFile()) {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), DEFAULT_CHARSET);
                BufferedReader bufferedReader = new BufferedReader(read);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    addUser(line);
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

    public void addUser(String string) {
        String[] paramters = string.split("@");
        switch (paramters.length) {
        case 1:
            addUser(paramters[0], null, null);
            break;
        case 2:
            addUser(paramters[0], null, paramters[1]);
            break;
        case 3:
            addUser(paramters[0], paramters[1], paramters[2]);
            break;
        }
    }

    public synchronized void addUser(String name, String password, String path) {
        if (null != ftpserver) {
            ftpserver.addUser(name, password, path);
        }
    }

    public synchronized void shutdown() {
        shutdownFtpserver();
    }

    public synchronized boolean shutdownControllerSocketServer() {
        if (null != ftpserver) {
            log.info("The server is shutting down!");
            return ftpserver.shutdownControllerSocketServer();
        } else {
            return false;
        }
    }

    public synchronized void shutdownFtpserver() {
        log.info("The server is shutting down!");
        if (null != ftpserver) {
            ftpserver.stop();
        }
        log.info("The server is already shutdown!");
    }

    public static void createServer(Socket socket, ThinFtpServerController controller) {
        new Thread(new FtpServerControllerSocketServer(socket, controller)).start();
    }
}