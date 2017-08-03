package com.xiaoleilei.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Socket服务端
 * 功能说明：信息共享
 * 模拟聊天室，实现多人聊天
 */
public class Server extends ServerSocket {

    private static final int SERVER_PORT = 8888; // 服务端端口
    private static final String END_MARK = "quit"; // 退出聊天室标识
    private static final String VIEW_USER = "checkuser"; // 查看在线成员列表

    private static List<String> userList = new CopyOnWriteArrayList<>();
    private static List<Task> threadList = new ArrayList<>(); // 服务器已启用线程集合
    private static BlockingQueue<String> msgQueue = new ArrayBlockingQueue<>(
            20); // 存放消息的队列

    public Server() throws Exception {
        super(SERVER_PORT);
    }

    /**
     * 启动向客户端发送消息的线程，使用线程处理每个客户端发来的消息
     *
     * @throws Exception
     */
    public void load() throws Exception {
        new Thread(new PushMsgTask()).start(); // 开启向客户端发送消息的线程

        while (true) {
            Socket socket = this.accept();
            // 每接收到一个Socket就建立一个新的线程来处理它
            new Thread(new Task(socket)).start();
        }
    }

    /**
     * 从消息队列中取消息，再发送给聊天室所有成员
     */
    class PushMsgTask implements Runnable {

        @Override
        public void run() {
            while (true) {
                String msg = null;
                try {
                    msg = msgQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (msg != null) {
                    for (Task thread : threadList) {
                        thread.sendMsg(msg);
                    }
                }
            }
        }

    }

    /**
     * 处理客户端发来的消息线程类
     */
    class Task implements Runnable {

        private Socket socket;

        private BufferedReader buff;

        private Writer writer;

        private String userName; // 成员名称

        /**
         * 处理客户端的消息，加入到在线成员列表中
         */
        public Task(Socket socket) {
            this.socket = socket;
            this.userName = String.valueOf(socket.getPort());
            try {
                this.buff = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), "UTF-8"));
                this.writer = new OutputStreamWriter(socket.getOutputStream(),
                        "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            userList.add (this.userName);
            threadList.add(this);
            pushMsg("【" + this.userName + "进入了聊天室】");
            System.out.println("客户端[port:" + socket.getPort() + "] "
                    + this.userName + "进入了聊天室");
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String msg = buff.readLine();

                    if (VIEW_USER.equals(msg)) { // 查看聊天室在线成员
                        sendMsg(onlineUsers());
                    } else if (END_MARK.equals(msg)) { // 遇到退出标识时就结束让客户端退出
                        sendMsg(END_MARK);
                        break;
                    } else {
                        System.out.println("[" + userName + "]说:" + msg + "\r\n");
                        pushMsg("[" + userName + "]说:" + msg );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally { // 关闭资源，聊天室移除成员
                try {
                    writer.close();
                    buff.close();
                    socket.close();
                } catch (Exception e) {

                }
                userList.remove(userName);
                threadList.remove(this);
                pushMsg("【" + userName + "退出了聊天室】");
                System.out.println("客户端[port:" + socket.getPort() + "] "
                        + userName + "退出了聊天室");
            }
        }

        /**
         * 准备发送的消息存入队列
         */
        private void pushMsg(String msg) {
            try {
                msgQueue.put(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * 发送消息
         */
        private void sendMsg(String msg) {
            try {
                writer.write(msg);
                writer.write("\r\n");
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 聊天室在线成员列表
         */
        private String onlineUsers() {
            StringBuffer sbf = new StringBuffer();
            sbf.append("======== 在线成员列表(").append(userList.size())
                    .append(") ========\r\n");
            for (int i = 0; i < userList.size(); i++) {
                sbf.append("[" + userList.get(i) + "]\r\n");
            }
            sbf.append("===============================");
            return sbf.toString();
        }

    }

    public static void main(String[] args) {
        try {
            Server server = new Server(); // 启动服务端
            server.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}