package com.leisurexi.http.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * NIO socket server demo.
 *
 * @author: leisurexi
 * @date: 2020-11-09 21:37
 */
public class NioSocketServer {

    public void start(int port) {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            // 设置为非阻塞模式
            serverSocketChannel.configureBlocking(false);
            // 注册 selector
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务端启动...");

            ServerHandler serverHandler = new ServerHandler();
            while (true) {
                // 阻塞等待事件发生
                selector.select();

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    try {
                        // 连接请求
                        if (selectionKey.isAcceptable()) {
                            serverHandler.handleAccept(selectionKey);
                        }
                        // 读请求
                        if (selectionKey.isReadable()) {
                            serverHandler.handlerRead(selectionKey);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 处理完移除当前 key
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ServerHandler {

        public void handleAccept(SelectionKey selectionKey) throws IOException {
            SocketChannel channel = ((ServerSocketChannel) selectionKey.channel()).accept();
            // 设置为非阻塞模式
            channel.configureBlocking(false);
            // 注册为 selector
            channel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
            System.out.println("建立连接....");
        }

        public String handlerRead(SelectionKey selectionKey) throws IOException {
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();

            String receivedStr = "";
            if (channel.read(buffer) == -1) {
                // 没读到内容关闭
                channel.shutdownInput();
                channel.shutdownOutput();
                channel.close();
                System.out.println("连接断开...");
            } else {
                // 将 channel 改为读取状态
                buffer.flip();
                // 按照编码读取数据
                receivedStr = Charset.forName("UTF-8").newDecoder().decode(buffer).toString();
                buffer.clear();

                // 返回数据给客户端
                buffer = buffer.put(("received string : " + receivedStr).getBytes());
                // 读取模式
                buffer.flip();
                channel.write(buffer);
                // 注册 selector 继续读取数据
                channel.register(selectionKey.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
            }
            return receivedStr;
        }

    }

    public static void main(String[] args) {
        NioSocketServer nioSocketServer = new NioSocketServer();
        nioSocketServer.start(8080);
    }

}
