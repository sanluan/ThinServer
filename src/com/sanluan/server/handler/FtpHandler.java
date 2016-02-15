package com.sanluan.server.handler;

import static com.sanluan.server.log.Log.getLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.sanluan.server.ThinFtpServer;
import com.sanluan.server.ThinFtpServer.User;
import com.sanluan.server.base.ThinFtp;
import com.sanluan.server.log.Log;

public class FtpHandler implements Runnable, ThinFtp {
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final DateFormat LIST_DATE_FORMAT = new SimpleDateFormat("MM dd HH:mm");
    public static final DateFormat LIST_DATE_FORMAT_ = new SimpleDateFormat("MM dd yyyy");
    private ThinFtpServer server;
    private Socket socket; // 用于控制的套接字
    private Socket transportSocket; // 用于传输的套接字
    private ServerSocket transportServerSocket; // 用于传输的套接字
    private boolean isPasv = false;
    private User user;
    private String currentPath = "/";// 当前目录
    private String rootPath; // 根目录
    private int state = State.STATE_NEED_USERNAME; // 用户状态标识符,在checkPASS中设置
    private BufferedReader input;
    private PrintWriter output;
    private int type = 0; // 文件类型(ascII 或 bin)
    final Log log = getLog(getClass());

    public FtpHandler(Socket socket, String rootPath, ThinFtpServer server) {
        this.socket = socket;
        this.rootPath = rootPath;
        this.server = server;
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            log.error("The ftp server start error:" + e.getMessage());
        }
    }

    public void run() {
        try {
            boolean flag = true;
            String inputString = "";
            output.println("332 welcome to ThinServer.");
            output.flush();
            while (flag && null != (inputString = input.readLine())) {
                String command;
                String param = "";
                int index = inputString.indexOf(BLANKSPACE);
                if (-1 == index) {
                    command = inputString.toUpperCase();
                } else {
                    command = inputString.substring(0, index).toUpperCase();
                    if (index < inputString.length()) {
                        param = inputString.substring(index + 1, inputString.length());
                    }
                }
                switch (state) {
                case State.STATE_NEED_USERNAME:
                    flag = checkUsername(command, param);
                    break;
                case State.STATE_NEED_PASSWORD:
                    flag = checkPassword(command, param);
                    break;
                case State.STATE_READY:
                    switch (command) {
                    case "ABOR": // 中断数据连接程序
                        try {
                            if (null != transportSocket) {
                                transportSocket.close();
                                if (isPasv) {
                                    transportServerSocket.close();
                                }
                            }
                        } catch (Exception e) {
                            output.println("451 failed to send.");
                        }
                        output.println("421 service unavailable.");
                        break;
                    case "ACCT":// 系统特权帐号
                        output.println("500 command not supported.");
                        break;
                    case "ALLO":// 为服务器上的文件存储器分配字节
                        output.println("500 command not supported.");
                        break;
                    case "APPE":// 添加文件到服务器同名文件
                        output.println("500 command not supported.");
                        break;
                    case "CDUP":// 到上一层目录
                        changeCurrentPath("..");
                        break;
                    case "CWD": // 到指定的目录
                        changeCurrentPath(param);
                        break;
                    case "DELE": // 删除指定文件
                        deleteFile(param);
                        break;
                    case "HELP": // 返回指定命令信息
                        output.println("500 command not supported.");
                        break;
                    case "LIST": // 如果是文件名列出文件信息，如果是目录则列出文件列表
                    case "NLST": // 列出指定目录内容
                        listFiles(param);
                        break;
                    case "MODE":
                        output.println("500 command not supported.");
                        break;
                    case "MDTM":
                        lastModified(param);
                        break;
                    case "MKD": // 建立目录
                        makeDir(param);
                        break;
                    case "NOOP":
                        output.println("200 ok.");
                        break;
                    case "PASV":
                        pasvMode();
                        break;
                    case "PORT": // IP 地址和两字节的端口 ID
                        portMode(param);
                        break;
                    case "PWD":
                    case "XPWD": // "当前目录" 信息
                        output.println("257 \"" + currentPath + "\"");
                        break;
                    case "QUIT": // 退出
                        output.println("221 close.");
                        if (null != transportSocket) {
                            transportSocket.close();
                        }
                        flag = false;
                        break;
                    case "REIN":
                        output.println("500 command not supported.");
                        break;
                    case "REST":
                        output.println("500 command not supported.");
                        break;
                    case "RETR": // 从服务器中获得文件
                        getFile(param);
                        break;
                    case "RMD": // 删除指定目录
                        deleteDir(param);
                        break;
                    case "RNFR": // 对旧路径重命名
                        output.println("500 command not supported.");
                        break;
                    case "RNTO": // 对旧路径重命名
                        output.println("500 command not supported.");
                        break;
                    case "SITE": // 由服务器提供的站点特殊参数
                        output.println("500 command not supported.");
                        break;
                    case "SIZE": // 文件大小
                        size(param);
                        break;
                    case "SMNT": // 挂载指定文件结构
                        output.println("500 command not supported.");
                        break;
                    case "STAT": // 在当前程序或目录上返回信息
                        output.println("500 command not supported.");
                        break;
                    case "STOR":// 储存（复制）文件到服务器上
                        reciveFile(param);
                        break;
                    case "STOU":// 储存文件到服务器名称上
                        output.println("500 command not supported.");
                        break;
                    case "STRU":// 数据结构（F=文件，R=记录，P=页面）
                        output.println("500 command not supported.");
                        break;
                    case "SYST":
                        output.println("215 " + System.getProperty("os.name"));
                        break;
                    case "TYPE":// 数据类型（A=ASCII,E=EBCDIC,I=binary）
                        if (param.equals("A")) {
                            type = State.TYPE_ASCII;
                            output.println("200 changed to ASCII.");
                        } else if (param.equals("I")) {
                            type = State.TYPE_IMAGE;
                            output.println("200 changed to BINARY.");
                        } else {
                            output.println("504 error paramter.");
                        }
                        break;
                    case "FEAT":
                        output.println("extension supported:");
                        output.println("MDTM");
                        output.println("SIZE");
                        output.println("PASV");
                        output.println("211 ok.");
                        break;
                    default:
                        output.println("500 error command.");
                        break;
                    }
                    break;
                }
            }
            output.flush();
            input.close();
            output.close();
            socket.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private boolean checkUsername(String command, String username) {
        if ("USER".equals(command)) {
            user = server.getUser(username);
            if (null != user) {
                if (null == user.getPassword()) {
                    welcome();
                } else {
                    output.println("331 need password.");
                    state = State.STATE_NEED_PASSWORD;
                }
                return true;
            } else {
                output.println("501 user does't exist.");
            }
        } else {
            output.println("501 error command,need login.");
        }
        return false;
    }

    private void welcome() {
        state = State.STATE_READY;
        output.println("230 login success,welcome " + user.getName() + ".");
    }

    private boolean checkPassword(String command, String password) {
        if ("PASS".equals(command)) {
            if (null != password && password.equalsIgnoreCase(user.getPassword()) || null == user.getPassword()
                    || 0 < user.getPassword().length()) {
                welcome();
                return true;
            } else {
                output.println("530 login failed.");
            }
        } else {
            output.println("501 error command,need password.");
        }
        return false;
    }

    private void pasvMode() {
        try {
            transportServerSocket = new ServerSocket(0, 1, socket.getLocalAddress());
            InetAddress inetAddress = transportServerSocket.getInetAddress();
            if (inetAddress.isAnyLocalAddress()) {
                inetAddress = socket.getLocalAddress();
            }
            String str = "";
            byte[] arrayOfByte = inetAddress.getAddress();
            for (int i = 0; i < arrayOfByte.length; ++i) {
                str = str + (arrayOfByte[i] & 0xFF) + ",";
            }
            str = str + (transportServerSocket.getLocalPort() >>> 8 & 0xFF) + "," + (transportServerSocket.getLocalPort() & 0xFF);
            output.println("227  entering passive mode (" + str + ")");
            output.flush();
            transportSocket = transportServerSocket.accept();
            isPasv = true;
        } catch (Exception e) {
            output.println("451 failed to send.");
        }
    }

    private void portMode(String param) {
        int p1 = 0;
        int p2 = 0;
        int[] a = new int[6];
        int j = 0;
        try {
            while ((p2 = param.indexOf(",", p1)) != -1) {
                a[j] = Integer.parseInt(param.substring(p1, p2));
                p2 = p2 + 1;
                p1 = p2;
                j++;
            }
            a[j] = Integer.parseInt(param.substring(p1, param.length()));// 最后一位
        } catch (NumberFormatException e) {
            output.println("501 error command.");
        }
        try {
            transportSocket = new Socket(a[0] + "." + a[1] + "." + a[2] + "." + a[3], a[4] * 256 + a[5],
                    InetAddress.getLocalHost(), 20);
            output.println("200 ok.");
        } catch (IOException e) {
            output.println("451 failed to send.");
        }
    }

    private void getFile(String param) {
        File file = new File(getCurrentPath(param));
        if (file.exists()) {
            try {
                if (type == State.TYPE_IMAGE) {
                    output.println("150 Opening ASCII mode data connection for " + param);
                    BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
                    PrintStream dataOutput = new PrintStream(transportSocket.getOutputStream(), true);
                    byte[] buf = new byte[1024];
                    int l = 0;
                    while ((l = fin.read(buf, 0, 1024)) != -1) {
                        dataOutput.write(buf, 0, l);
                    }
                    fin.close();
                    dataOutput.close();
                } else {
                    output.println("150 Opening ASCII mode data connection for " + param);
                    BufferedReader fin = new BufferedReader(new FileReader(file));
                    PrintWriter dataOutput = new PrintWriter(transportSocket.getOutputStream(), true);
                    String s;
                    while ((s = fin.readLine()) != null) {
                        dataOutput.println(s);
                    }
                    fin.close();
                    dataOutput.close();
                }
                transportSocket.close();
                if (isPasv) {
                    transportServerSocket.close();
                }
                output.println("226 send completed.");
            } catch (Exception e) {
                output.println("451 failed to send.");
            }
        } else {
            output.println("550 file does't exist");
        }
    }

    private void lastModified(String param) {
        File file = new File(getCurrentPath(param));
        if (file.exists()) {
            output.println("213 " + DATE_FORMAT.format(new Date(file.lastModified())));
        } else {
            output.println("550 file does't exist");
        }
    }

    private void size(String param) {
        File file = new File(getCurrentPath(param));
        if (file.exists()) {
            output.println("213 " + file.length());
        } else {
            output.println("550 file does't exist");
        }
    }

    private void reciveFile(String param) {
        if (!"".equals(param)) {
            try {
                if (type == State.TYPE_IMAGE) {
                    output.println("150 Opening Binary mode data connection for " + param);
                    BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(getCurrentPath(param)));
                    BufferedInputStream dataInput = new BufferedInputStream(transportSocket.getInputStream());
                    byte[] buf = new byte[1024];
                    int l = 0;
                    while ((l = dataInput.read(buf, 0, 1024)) != -1) {
                        fout.write(buf, 0, l);
                    }
                    dataInput.close();
                    fout.close();
                } else {
                    output.println("150 Opening ASCII mode data connection for " + param);
                    PrintWriter fout = new PrintWriter(new FileOutputStream(getCurrentPath(param)));
                    BufferedReader dataInput = new BufferedReader(new InputStreamReader(transportSocket.getInputStream()));
                    String line;
                    while ((line = dataInput.readLine()) != null) {
                        fout.println(line);
                    }
                    dataInput.close();
                    fout.close();
                }
                transportSocket.close();
                if (isPasv) {
                    transportServerSocket.close();
                }
                output.println("226 send completed.");
            } catch (Exception e) {
                output.println("451 failed to send.");
            }
        } else {
            output.println("501 error paramter.");
        }
    }

    private String dealPath(String path) {
        int index;
        while (0 <= (index = path.indexOf(".."))) {
            int pindex = path.substring(0, index).lastIndexOf("/", index - 2);
            if (0 <= pindex) {
                path = path.substring(0, pindex) + path.substring(index + 2);
            } else {
                path = path.substring(index + 2);
            }
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path.replace("//", "/");
    }

    private void changeCurrentPath(String path) {
        File directory = new File(getCurrentPath(path));
        if (directory.exists() && directory.isDirectory()) {
            path = (null == path ? "/" : path.startsWith("/") ? path : currentPath + path + "/");
            currentPath = dealPath(path);
            output.println("250 current directory changed to " + currentPath);
        } else {
            output.println("550 path does't exist.");
        }
    }

    private void makeDir(String path) {
        File dir = new File(getCurrentPath(path));
        if (dir.exists()) {
            output.println("550 directory already exists.");
        } else {
            dir.mkdirs();
            output.println("250 directory created.");
        }
    }

    private void deleteDir(String path) {
        File dir = new File(getCurrentPath(path));
        if (dir.exists() && dir.isDirectory()) {
            dir.delete();
            output.println("250 directory deleted.");
        } else {
            output.println("550 directory does't exist.");
        }
    }

    private void deleteFile(String path) {
        File file = new File(getCurrentPath(path));
        if (file.exists() && !file.isDirectory()) {
            file.delete();
            output.println("250 file deleted.");
        } else {
            output.println("550 file or directory does't exist.");
        }
    }

    private void listFiles(String param) {
        String path;
        if (null == param || param.startsWith("-")) {
            path = getCurrentPath(null);
        } else {
            path = getCurrentPath(param);
        }
        try {
            PrintWriter dout = new PrintWriter(transportSocket.getOutputStream(), true);
            output.println("150 Opening ASCII mode data connection.");
            DirectoryStream<Path> stream = null;
            try {
                stream = Files.newDirectoryStream(Paths.get(path));
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                for (Path entry : stream) {
                    File file = entry.toFile();
                    StringBuffer sb = new StringBuffer();
                    sb.append(file.isDirectory() ? 'd' : !file.getAbsolutePath().equals(file.getCanonicalPath()) ? 'l' : '-');
                    StringBuffer sb1 = new StringBuffer();
                    sb1.append(file.canRead() ? 'r' : '-').append(file.canWrite() ? 'w' : '-')
                            .append(file.canExecute() ? 'x' : '-');
                    String rightString = sb1.toString();
                    sb.append(rightString).append(rightString).append(rightString);
                    cal.setTimeInMillis(file.lastModified());
                    sb.append(BLANKSPACE)
                            .append(file.isDirectory() ? file.listFiles().length : 1)
                            .append(BLANKSPACE)
                            .append("0 0")
                            .append(BLANKSPACE)
                            .append(String.valueOf(file.length()))
                            .append(BLANKSPACE)
                            .append(year == cal.get(Calendar.YEAR) ? LIST_DATE_FORMAT.format(cal.getTime()) : LIST_DATE_FORMAT_
                                    .format(cal.getTime())).append(BLANKSPACE);
                    sb.append(entry.getFileName().toString());
                    dout.println(sb.toString());
                }
                dout.flush();
            } catch (IOException e) {
            } finally {
                try {
                    if (null != stream) {
                        stream.close();
                    }
                } catch (IOException e) {
                }
            }
            dout.close();
            transportSocket.close();
            if (isPasv) {
                transportServerSocket.close();
            }
            output.println("226 send completed.");
        } catch (Exception e) {
            output.println("451 failed to send.");
        }
    }

    private String getCurrentPath(String path) {
        if (null == path) {
            path = currentPath;
        } else {
            if (!path.startsWith("/")) {
                path = currentPath + path;
            }
            if (!path.endsWith("/")) {
                path += "/";
            }
        }
        return rootPath + user.getPath() + path;
    }

    private class State {
        public final static int STATE_NEED_USERNAME = 0; // 需要用户名
        public final static int STATE_NEED_PASSWORD = 1; // 需要密码
        public final static int STATE_READY = 2; // 已经登陆状态

        public final static int TYPE_ASCII = 0;
        public final static int TYPE_IMAGE = 1;
    }
}