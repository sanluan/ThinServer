package com.sanluan.server.socket;

import static com.sanluan.server.log.Log.getLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.sanluan.server.Thin;
import com.sanluan.server.ThinServerController;
import com.sanluan.server.log.Log;

public class ClientServerController implements Thin {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String password;
    final Log log = getLog(getClass());

    public ClientServerController(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (null != password) {
                log.info(input.readLine());
                output.println(password);
            }
            readLine();
        } catch (IOException e) {
            log.error("The server master start error:" + e.getMessage());
        }
    }

    private void readLine() {
        try {
            log.info(input.readLine());
        } catch (IOException e) {
        }
    }

    public void reLoad(String path) {
        output.println(ThinServerController.COMMOND_RELOAD + BLANKSPACE + path);
        readLine();
    }

    public void unLoad(String path) {
        output.println(ThinServerController.COMMOND_UNLOAD + BLANKSPACE + path);
        readLine();
    }

    public void load(String path, String webappPath) {
        output.println(ThinServerController.COMMOND_LOAD + BLANKSPACE + path + BLANKSPACE + webappPath);
        readLine();
    }

    public void load(String path) {
        output.println(ThinServerController.COMMOND_LOAD + BLANKSPACE + path);
        readLine();
    }

    public void shutdown() {
        output.println(ThinServerController.COMMOND_SHUTDOWN);
        readLine();
    }

    public void close() {
        output.println("bye");
        readLine();
        try {
            input.close();
        } catch (IOException e) {
        }
        output.close();
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
