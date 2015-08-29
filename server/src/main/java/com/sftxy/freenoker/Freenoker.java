package com.sftxy.freenoker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sftxy.freenoker.engine.TemplateEngine;

public class Freenoker {
    private static Logger logger = Logger.getLogger(Freenoker.class.getCanonicalName());

    private static final int DEFAULT_PORT = 65384;

    private int port = DEFAULT_PORT;

    private AsynchronousChannelGroup group;
    private AsynchronousServerSocketChannel server;

    private TemplateEngine tmplEngine;

    private boolean running;

    public static void main(String[] args) {
        Map<String, String> parsedArgs = parseArgs(args);

        Freenoker freenoker = new Freenoker();

        freenoker.initTemplateEngine(parsedArgs);

        freenoker.start();
    }

    private void initTemplateEngine(Map<String, String> args) {
        tmplEngine = new TemplateEngine();
        tmplEngine.init(args);
    }

    private void start() {
        InetSocketAddress localAddress = new InetSocketAddress(port);
        try {
            group = AsynchronousChannelGroup.withThreadPool(Executors.newSingleThreadExecutor());
            server = AsynchronousServerSocketChannel.open(group).bind(localAddress);
        } catch (IOException e) {
            logger.warning("failed to init server, error: " + e.getLocalizedMessage());
        }

        if (null == group || null == server) {
            return;
        }

        running = true;
        try {
            server.accept(null, acceptHandler);
            group.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception e) {
            running = false;
            logger.warning("exception occured when executing: " + e.getLocalizedMessage());
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                logger.warning("failed to close server, error: " + e.getLocalizedMessage());
            }
        }
    }

    private CompletionHandler<AsynchronousSocketChannel, Void> acceptHandler = new CompletionHandler<AsynchronousSocketChannel, Void>() {
        @Override
        public void completed(AsynchronousSocketChannel worker, Void attachment) {
            server.accept(null, this); // listen to next request

            ByteBuffer buffer = ByteBuffer.allocate(32);
            worker.read(buffer, null, new CompletionHandler<Integer, Void>() {
                @Override
                public void completed(Integer result, Void attachment) {
                    if (result < 0) {
                        buffer.clear();
                        try {
                            worker.close();
                        } catch (IOException e) {
                            logger.info("error occured when close worker, error: " + e.getLocalizedMessage());
                        }
                        return;
                    }

                    String tmplName = buildTmplName(result, buffer);

                    buffer.clear();
                    worker.read(buffer, null, this); // keep live

                    byte[] content = tmplEngine.render(tmplName);
                    worker.write(ByteBuffer.wrap(content));
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    String error = "failed when read buffer, error: " + exc.getLocalizedMessage();
                    logger.warning(error);

                    buffer.clear();
                    try {
                        worker.close();
                    } catch (IOException e) {
                        logger.info("error occured when close worker, error: " + e.getLocalizedMessage());
                    }
                }
            });
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
            logger.warning("failed when accept connection, error: " + exc.getLocalizedMessage());
        }
    };

    public boolean isRunning() {
        return running;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private static String buildTmplName(Integer result, ByteBuffer buffer) {
        byte[] content = new byte[result];
        for (int i = 0; i < result; i++) {
            content[i] = buffer.get(i);
        }

        return new String(content).trim();
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            String[] kv = arg.split("=", 2);
            map.put(kv[0], kv[1]);
        }
        return map;
    }
}
