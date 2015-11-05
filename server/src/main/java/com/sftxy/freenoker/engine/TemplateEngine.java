package com.sftxy.freenoker.engine;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.core.Environment;
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
        config.setCacheStorage(NullCacheStorage.INSTANCE);

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

    public byte[] render(String tmplName) throws Exception {
        if (enableLayout) {
            return renderLayoutedTmpl(tmplName);
        } else {
            return doRender(null, tmplName);
        }
    }

    private byte[] renderLayoutedTmpl(String tmplName) throws Exception {
        Template screenContentTemplate = getTemplate(tmplName);
        StringWriter sw = new StringWriter();
        Environment env = screenContentTemplate.createProcessingEnvironment(null, sw);
        env.process();

        Map<String, Object> model = new HashMap<>();
        model.put(screenContentKey, sw.toString());

        String layoutToUse = Objects.toString(model.get(layoutKey), layoutLocation);

        return doRender(model, layoutToUse);
    }

    private byte[] doRender(Map<String, Object> model, String tmplName) throws Exception {
        Template template = getTemplate(tmplName);
        byte[] content = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        try {
            template.process(model, writer);
            content = out.toByteArray();
        } catch (TemplateException | IOException e) {
            String error = "failed to process template, error: " + e.getLocalizedMessage();
            logger.warning(error);
            throw new Exception(error, e);
        }
        return content;

    }

    private Template getTemplate(String tmplName) throws Exception {
        Template template = null;
        try {
            template = config.getTemplate(tmplName + ".ftl");
        } catch (IOException e) {
            String error = "incorrect template name, error: " + e.getLocalizedMessage();
            logger.warning(error);
            throw new Exception(error, e);
        }
        return template;
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
