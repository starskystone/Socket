package com.xiaoleilei.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * Socket客户端
 * 功能说明：信息共享
 * 模拟聊天室，实现多人聊天
 */
public class Client extends Socket {

    private static final String SERVER_IP = "10.101.219.45"; // 服务端IP
    private static final int SERVER_PORT = 8888; // 服务端端口
    private static final String END_MARK = "quit"; // 退出聊天室标识

    private Socket client;

    private Writer writer;

    // 发送消息输入流
    private BufferedReader in;

    /**
     * 构造函数
     * 与服务器建立连接
     */
    public Client() throws Exception {
        super(SERVER_IP, SERVER_PORT);
        this.client = this;
        System.out.println("Cliect[port:" + client.getLocalPort() + "] 您已进入聊天室");
    }

    /**
     * 启动监听收取消息，循环可以不停的输入消息，将消息发送出去
     */
    public void init() throws Exception {
        this.writer = new OutputStreamWriter(this.getOutputStream(), "UTF-8");
        new Thread(new ReceiveMsgTask()).start(); // 启动监听

        while(true) {
            in = new BufferedReader(new InputStreamReader(System.in));
            String inputMsg = in.readLine();
            writer.write(inputMsg);
            writer.write("\n");
            writer.flush(); // 写完后要记得flush
        }
    }

    /**
     * 监听服务器发来的消息线程类
     */
    class ReceiveMsgTask implements Runnable {

        private BufferedReader buff;

        @Override
        public void run() {
            try {
                this.buff = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                while (true) {
                    String result = buff.readLine();
                    if (END_MARK.equals(result)) { // 遇到退出标识时表示服务端返回确认退出
                        System.out.println("Cliect[port:" + client.getLocalPort() + "] 您已退出聊天室");
                        break;
                    } else {
                        System.out.println(result);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    buff.close();
                    writer.close();
                    client.close();
                    in.close();
                } catch (Exception e) {

                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client(); // 启动客户端
            client.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

