package com.sftxy.freenoker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Freenoker {

    private static final int DEFAULT_PORT = 65384;

    private int port;

    private AsynchronousServerSocketChannel server;

    private boolean running;

    public static void main(String[] args) {
        Freenoker freenoker = null;
        if (args.length > 0) {
            freenoker = new Freenoker(Integer.parseInt(args[0]));
        } else {
            freenoker = new Freenoker();
        }
        freenoker.start();
    }

    public Freenoker() {
        this(DEFAULT_PORT);
    }

    public Freenoker(int port) {
        this.port = port;
    }

    private void start() {
        InetSocketAddress localAddress = new InetSocketAddress(port);
        try {
            server = AsynchronousServerSocketChannel.open().bind(localAddress);
        } catch (IOException e) {
            System.out.println("failed to init server, error: " + e.getLocalizedMessage());
        }

        if (null == server) {
            return;
        }

        running = true;
        try {
            execute();
        } catch (Exception e) {
            running = false;
            System.out.println("exception occured when executing");
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                System.out.println("failed to close server, error: " + e.getLocalizedMessage());
            }
        }
    }

    private void execute() {
        while (running) {
            Future<AsynchronousSocketChannel> acceptFuture = server.accept();
            AsynchronousSocketChannel worker = null;
            try {
                worker = acceptFuture.get();
                ByteBuffer buffer = ByteBuffer.allocate(64);
                worker.read(buffer).get();
                String tmplName = new String(buffer.array());
                String tmpl = "loading " + tmplName + " template...";
                worker.write(ByteBuffer.wrap(tmpl.getBytes()));
            } catch (ExecutionException | InterruptedException e) {
                System.out.println("failed to get data, error: " + e.getLocalizedMessage());
            } finally {
                if (null != worker) {
                    try {
                        worker.close();
                    } catch (IOException e) {
                        System.out.println("failed to close worker, error: " + e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
