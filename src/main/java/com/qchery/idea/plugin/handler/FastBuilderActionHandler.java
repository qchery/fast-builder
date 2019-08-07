package com.qchery.idea.plugin.handler;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.generate.GenerationUtil;
import org.jetbrains.java.generate.exception.GenerateCodeException;
import org.jetbrains.java.generate.template.TemplateResource;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Chery
 * @date 2019/7/29
 */
public class FastBuilderActionHandler implements CodeInsightActionHandler {

    private Logger log = Logger.getInstance(FastBuilderActionHandler.class);

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!EditorModificationUtil.checkModificationAllowed(editor)) return;
        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }
        final PsiClass aClass = OverrideImplementUtil.getContextClass(project, editor, file, false);
        if (aClass == null || aClass.isInterface()) return; //?
        log.assertTrue(aClass.isValid());
        log.assertTrue(aClass.getContainingFile() != null);

        CommandProcessor.getInstance().executeCommand(project, () -> {
            final int offset = editor.getCaretModel().getOffset();
            try {
                doGenerate(project, aClass);
            } catch (GenerateCodeException e) {
                final String message = e.getMessage();
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!editor.isDisposed()) {
                        editor.getCaretModel().moveToOffset(offset);
                        HintManager.getInstance().showErrorHint(editor, message);
                    }
                }, project.getDisposed());
            }
        }, null, null);
    }

    private void doGenerate(@NotNull Project project, PsiClass aClass) {
        TemplateResource defaultTemplate = BuildTemplatesManager.getInstance().getDefaultTemplate();

        String generateCode = GenerationUtil.velocityGenerateCode(aClass, Arrays.asList(aClass.getFields()), Collections.emptyMap(),
                defaultTemplate.getTemplate(), 0, false);
        PsiClass builderClass = JavaPsiFacade.getElementFactory(project)
                .createClassFromText(generateCode, aClass).getInnerClasses()[0];
        aClass.add(builderClass);

        // generate new Builder method
        PsiMethod newBuilderMethod = JavaPsiFacade.getElementFactory(project).createMethodFromText("public static Builder builder() {\n" +
                "        return new Builder();\n" +
                "    }", aClass);
        aClass.add(newBuilderMethod);
    }

}
