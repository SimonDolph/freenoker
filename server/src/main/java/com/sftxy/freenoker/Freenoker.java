package com.sftxy.freenoker;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
            server = AsynchronousServerSocketChannel.open().bind(localAddress);
        } catch (IOException e) {
            logger.warning("failed to init server, error: " + e.getLocalizedMessage());
        }

        if (null == server) {
            return;
        }

        running = true;
        try {
            execute();
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

    private void execute() {
        while (running) {
            Future<AsynchronousSocketChannel> acceptFuture = server.accept();
            AsynchronousSocketChannel worker = null;
            try {
                worker = acceptFuture.get();
                ByteBuffer buffer = ByteBuffer.allocate(32);
                worker.read(buffer).get();

                String tmplName = new String(buffer.array()).trim();
                Template template = config.getTemplate(tmplName + ".ftl");
                ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
                Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                template.process(null, writer);
                worker.write(ByteBuffer.wrap(out.toByteArray()));
            } catch (ExecutionException | InterruptedException | IOException | TemplateException e) {
                String error = "failed to get data, error: " + e.getLocalizedMessage();
                logger.warning(error);
                if (worker != null) {
                    worker.write(ByteBuffer.wrap(error.getBytes()));
                }
            } finally {
                if (null != worker) {
                    try {
                        worker.close();
                    } catch (IOException e) {
                        logger.warning("failed to close worker, error: " + e.getLocalizedMessage());
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
