package com.qchery.idea.plugin.handler;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.GetterSetterPrototypeProvider;
import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.codeInsight.generation.SetterTemplatesManager;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.generate.exception.GenerateCodeException;
import org.jetbrains.java.generate.template.TemplateResource;
import org.jetbrains.java.generate.template.TemplatesManager;

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
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        PsiClass builderClass = factory.createClass("Builder");

        PsiModifierList modifierList = builderClass.getModifierList();
        log.assertTrue(modifierList != null);
        modifierList.setModifierProperty(PsiModifier.STATIC, true);

        // add field
        for (PsiField psiField : aClass.getFields()) {
            builderClass.add(psiField);
        }

        TemplatesManager templatesManager = SetterTemplatesManager.getInstance();
        TemplateResource defaultTemplate = templatesManager.getDefaultTemplate();
        try {
            if (!"Builder".equals(defaultTemplate.getFileName())) {
                TemplateResource template = templatesManager.findTemplateByName("Builder");
                log.assertTrue(template != null);
                templatesManager.setDefaultTemplate(template);
            }

            // generate setter method
            for (PsiField psiField : builderClass.getFields()) {
                PsiMethod[] setMethod = GetterSetterPrototypeProvider.generateGetterSetters(psiField, false);
                builderClass.add(setMethod[0]);
            }

            // generate build method
            StringBuilder builder = new StringBuilder()
                    .append("public ").append(aClass.getName()).append(" build() {")
                    .append(aClass.getName()).append(" result = new ").append(aClass.getName()).append("();");
            for (PsiField psiField : builderClass.getFields()) {
                builder.append("result.").append(psiField.getName()).append(" = this.").append(psiField.getName()).append(";");
            }
            builder.append("return result;}");

            PsiMethod buildPsiMethod = JavaPsiFacade.getElementFactory(project)
                    .createMethodFromText(builder.toString(), builderClass);
            builderClass.add(buildPsiMethod);

            aClass.add(builderClass);

            // generate new Builder method
            PsiMethod newBuilderMethod = JavaPsiFacade.getElementFactory(project).createMethodFromText("public static Builder builder() {\n" +
                    "        return new Builder();\n" +
                    "    }", aClass);
            aClass.add(newBuilderMethod);

        } finally {
            templatesManager.setDefaultTemplate(defaultTemplate);
        }
    }

}
