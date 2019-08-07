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
 * @date 2019/8/7 11:26
 */
@State(name = "FastBuilderInitTemplates", storages = @Storage("fastBuilderInitTemplates.xml"))
public class BuilderInitTemplatesManager extends TemplatesManager {

    private static final String DEFAULT = "defaultBuilderInit.vm";

    public static BuilderInitTemplatesManager getInstance() {
        return ServiceManager.getService(BuilderInitTemplatesManager.class);
    }

    @Override
    public TemplateResource[] getDefaultTemplates() {
        try {
            return new TemplateResource[]{
                    new TemplateResource("Builder Init Default", readFile(DEFAULT), true),
            };
        } catch (IOException e) {
            throw new TemplateResourceException("Error loading default templates", e);
        }
    }

    protected static String readFile(String resource) throws IOException {
        return readFile(resource, BuilderInitTemplatesManager.class);
    }
}
