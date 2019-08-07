package com.qchery.idea.plugin.handler;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.java.generate.exception.TemplateResourceException;
import org.jetbrains.java.generate.template.TemplateResource;
import org.jetbrains.java.generate.template.TemplatesManager;

import java.io.IOException;

/**
 * @author Chery
 * @date 2019/8/6 18:35
 */
@State(name = "FastBuilderTemplates", storages = @Storage("fastBuilderTemplates.xml"))
public class BuilderTemplatesManager extends TemplatesManager {

    private static final String DEFAULT = "defaultBuilder.vm";

    public static BuilderTemplatesManager getInstance() {
        return ServiceManager.getService(BuilderTemplatesManager.class);
    }

    @Override
    public TemplateResource[] getDefaultTemplates() {
        try {
            return new TemplateResource[]{
                    new TemplateResource("Builder Default", readFile(DEFAULT), true),
            };
        } catch (IOException e) {
            throw new TemplateResourceException("Error loading default templates", e);
        }
    }

    protected static String readFile(String resource) throws IOException {
        return readFile(resource, BuilderTemplatesManager.class);
    }
}
