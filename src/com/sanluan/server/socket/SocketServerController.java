package com.sanluan.server.socket;

import static org.apache.commons.logging.LogFactory.getLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.logging.Log;

import com.sanluan.server.Thin;
import com.sanluan.server.ThinServerController;

public class SocketServerController implements Runnable, Thin {
    private Socket socket;
    private ThinServerController controller;
    private BufferedReader input;
    private PrintWriter output;
    private String password;
    final Log log = getLog(getClass());

    public SocketServerController(Socket socket, ThinServerController controller) {
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
                    case ThinServerController.COMMOND_SHUTDOWN:
                        controller.shutdownHttpserver();
                        close();
                        if (controller.shutdownServerSocketController()) {
                            flag = false;
                        }
                        break;
                    case ThinServerController.COMMOND_LOAD:
                        switch (commonds.length) {
                        case 2:
                            controller.load(commonds[1]);
                            break;
                        case 3:
                            controller.load(commonds[1], commonds[2]);
                            break;
                        case 4:
                            controller.load(commonds[1], commonds[2], commonds[3]);
                            break;
                        default:
                            output.println("Invalid number of load parameters");
                        }
                        break;
                    case ThinServerController.COMMOND_UNLOAD:
                        if (1 < commonds.length) {
                            controller.unLoad(commonds[1]);
                        } else {
                            output.println("Invalid number of load parameters");
                        }
                        break;
                    case ThinServerController.COMMOND_RELOAD:
                        if (1 < commonds.length) {
                            controller.reLoad(commonds[1]);
                        } else {
                            output.println("Invalid number of load parameters");
                        }
                        break;
                    case ThinServerController.COMMOND_BYE:
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