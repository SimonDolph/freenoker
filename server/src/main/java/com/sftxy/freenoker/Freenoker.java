package com.sftxy.freenoker;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class Freenoker {
    private static Logger logger = Logger.getLogger(Freenoker.class.getCanonicalName());

    private static final int DEFAULT_PORT = 65384;

    private int port;

    private String templateLoaderPath;

    private Configuration config;

    private AsynchronousChannelGroup group;
    private AsynchronousServerSocketChannel server;

    private boolean running;

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("templateLoaderPath must be provided");
        }

        Freenoker freenoker = null;
        if (args.length == 2) {
            freenoker = new Freenoker(Integer.parseInt(args[0]), args[1]);
        } else {
            freenoker = new Freenoker(args[0]);
        }
        freenoker.initTemplateEngine().start();
    }

    private Freenoker initTemplateEngine() {
        File file = new File(templateLoaderPath);
        FileTemplateLoader templateLoader = null;
        try {
            templateLoader = new FileTemplateLoader(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("configured templateLoaderPath is not a valid file path, error: " + e.getLocalizedMessage());
        }

        config = new Configuration(Configuration.VERSION_2_3_23);
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        config.setTemplateLoader(templateLoader);

        return this;
    }

    public Freenoker(String templateLoaderPath) {
        this(DEFAULT_PORT, templateLoaderPath);
    }

    public Freenoker(int port, String templateLoaderPath) {
        this.port = port;
        this.templateLoaderPath = templateLoaderPath;
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
            server.accept(null, this);
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

                    byte[] content = new byte[result];
                    for (int i = 0; i < result; i++) {
                        content[i] = buffer.get(i);
                    }

                    buffer.clear();
                    worker.read(buffer, null, this);

                    String tmplName = new String(content).trim();
                    Template template = null;
                    try {
                        template = config.getTemplate(tmplName + ".ftl");
                    } catch (IOException e) {
                        String error = "incorrect template name, error: " + e.getLocalizedMessage();
                        logger.warning(error);
                        worker.write(ByteBuffer.wrap(error.getBytes()));
                    }

                    if (null == template) {
                        return;
                    }

                    ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
                    Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                    try {
                        template.process(null, writer);
                        worker.write(ByteBuffer.wrap(out.toByteArray()));
                    } catch (TemplateException | IOException e) {
                        String error = "failed to process template, error: " + e.getLocalizedMessage();
                        logger.warning(error);
                        worker.write(ByteBuffer.wrap(out.toByteArray()));
                    }
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

}
