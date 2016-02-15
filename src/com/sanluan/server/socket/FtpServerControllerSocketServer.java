package com.sanluan.server.socket;

import static com.sanluan.server.log.Log.getLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.sanluan.server.ThinFtpServerController;
import com.sanluan.server.base.ThinFtp;
import com.sanluan.server.log.Log;

public class FtpServerControllerSocketServer implements Runnable, ThinFtp {
    private Socket socket;
    private ThinFtpServerController controller;
    private BufferedReader input;
    private PrintWriter output;
    private String password;
    final Log log = getLog(getClass());

    public FtpServerControllerSocketServer(Socket socket, ThinFtpServerController controller) {
        this.socket = socket;
        this.controller = controller;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            log.error("The server master start error:" + e.getMessage());
        }
    }

    private void close() {
        output.println("bye~");
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

    public void run() {
        try {
            boolean flag = true;
            log.info(socket.getInetAddress().toString() + " try to control the server");
            if (null != password) {
                output.println("please input your password:");
                if (!password.equalsIgnoreCase(input.readLine())) {
                    flag = false;
                    close();
                }
            }
            while (flag) {
                output.println("what do you want to do?");
                String[] commonds = input.readLine().split(BLANKSPACE);
                if (null != commonds && 0 < commonds.length) {
                    log.info(socket.getInetAddress().toString() + " exec the commond :" + commonds[0]);
                    switch (commonds[0].toLowerCase()) {
                    case ThinFtpServerController.COMMOND_SHUTDOWN:
                        controller.shutdownFtpserver();
                        close();
                        if (controller.shutdownControllerSocketServer()) {
                            flag = false;
                        }
                        break;
                    case ThinFtpServerController.COMMOND_ADDUSER:
                        if (1 < commonds.length) {
                            controller.addUser(commonds[1]);
                        } else {
                            output.println("Invalid number of load parameters");
                        }
                        break;
                    case ThinFtpServerController.COMMOND_BYE:
                        flag = false;
                        close();
                        break;
                    default:
                        output.println("i cann't understand!");
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    socket = null;
                }
            }
        }
    }

    public void setPassword(String password) {
        this.password = password;
    }
}