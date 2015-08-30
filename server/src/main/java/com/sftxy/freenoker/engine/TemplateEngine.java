package com.sftxy.freenoker.engine;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class TemplateEngine {
    private static Logger logger = Logger.getLogger(TemplateEngine.class.getCanonicalName());

    private String templateLoaderPath;

    private Configuration config;

    public static final String DEFAULT_LAYOUT_LOCATION = "_layout.ftl";
    public static final String DEFAULT_LAYOUT_KEY = "layout";
    public static final String DEFAULT_SCREEN_CONTENT_KEY = "screen_content";

    private String layoutLocation;
    private String layoutKey;
    private String screenContentKey;

    private boolean enableLayout = false;

    /**
     * 
     * @param args path: $PATH layout: $LAYOUT
     */
    public void init(Map<String, String> args) {
        templateLoaderPath = args.get("path");
        if (null == templateLoaderPath) {
            throw new IllegalArgumentException("path must be provided");
        }

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

        setupLayoutFeature(args);
    }

    private void setupLayoutFeature(Map<String, String> args) {
        enableLayout = Boolean.parseBoolean(args.get("enableLayout"));
        if (enableLayout) {
            layoutLocation = Objects.toString(args.get("layout"), DEFAULT_LAYOUT_LOCATION);
            layoutKey = Objects.toString(args.get("layoutKey"), DEFAULT_LAYOUT_KEY);
            screenContentKey = Objects.toString(args.get("screenContentKey"), DEFAULT_SCREEN_CONTENT_KEY);
        }
    }

    public byte[] render(String tmplName) {
        if (enableLayout) {
            return doLayoutedRender(tmplName);
        } else {
            return doRender(tmplName);
        }
    }

    private byte[] doLayoutedRender(String tmplName) {
        return null;//TODO
    }

    private byte[] doRender(String tmplName) {
        byte[] content = null;
        Template template = null;
        try {
            template = config.getTemplate(tmplName + ".ftl");
        } catch (IOException e) {
            String error = "incorrect template name, error: " + e.getLocalizedMessage();
            logger.warning(error);
            content = error.getBytes(StandardCharsets.UTF_8);
        }

        if (null != template) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            try {
                template.process(null, writer);
                content = out.toByteArray();
            } catch (TemplateException | IOException e) {
                String error = "failed to process template, error: " + e.getLocalizedMessage();
                logger.warning(error);
                content = error.getBytes(StandardCharsets.UTF_8);
            }
        }
        return content;
    }

    public void setTemplateLoaderPath(String templateLoaderPath) {
        this.templateLoaderPath = templateLoaderPath;
    }

    public void setLayoutLocation(String layoutLocation) {
        this.layoutLocation = layoutLocation;
    }

    public void setLayoutKey(String layoutKey) {
        this.layoutKey = layoutKey;
    }

    public void setScreenContentKey(String screenContentKey) {
        this.screenContentKey = screenContentKey;
    }

}
