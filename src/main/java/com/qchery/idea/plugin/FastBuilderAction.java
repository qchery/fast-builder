package com.qchery.idea.plugin;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.qchery.idea.plugin.handler.FastBuilderActionHandler;

/**
 * @author Chery
 * @date 2019/7/29
 */
public class FastBuilderAction extends BaseGenerateAction {

    public FastBuilderAction() {
        super(new FastBuilderActionHandler());
    }
}
