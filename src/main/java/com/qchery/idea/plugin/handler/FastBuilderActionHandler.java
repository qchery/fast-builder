package com.qchery.idea.plugin.handler;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.generation.*;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.generate.GenerationUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Chery
 * @date 2019/7/29
 */
public class FastBuilderActionHandler extends GenerateGetterSetterHandlerBase {

    public FastBuilderActionHandler() {
        super("Select Fields to Generate Builder");
    }

    @Nullable
    @Override
    protected JComponent getHeaderPanel(Project project) {
        final JPanel panel = new JPanel(new BorderLayout(2, 2));
        String message = CodeInsightBundle.message("generate.equals.hashcode.template");
        panel.add(getHeaderPanel(project, BuilderTemplatesManager.getInstance(), message), BorderLayout.NORTH);
        panel.add(getHeaderPanel(project, BuilderInitTemplatesManager.getInstance(), message), BorderLayout.SOUTH);
        return panel;
    }

    @Nullable
    protected ClassMember[] chooseMembers(ClassMember[] members,
                                          boolean allowEmptySelection,
                                          boolean copyJavadocCheckbox,
                                          Project project,
                                          @Nullable Editor editor) {
        MemberChooser<ClassMember> chooser = createMembersChooser(members, allowEmptySelection, copyJavadocCheckbox, project);
        // choose all elements as default
        chooser.selectElements(members);
        chooser.show();
        myToCopyJavaDoc = chooser.isCopyJavadoc();
        final List<ClassMember> list = chooser.getSelectedElements();
        return list == null ? null : list.toArray(ClassMember.EMPTY_ARRAY);
    }

    @Override
    protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
        PsiField[] fields = aClass.getFields();
        ArrayList<ClassMember> array = new ArrayList<>();
        for (PsiField field : fields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (field.hasModifierProperty(PsiModifier.FINAL) && field.getInitializer() != null) continue;
            array.add(new PsiFieldMember(field));
        }
        return array.toArray(ClassMember.EMPTY_ARRAY);
    }

    @NotNull
    @Override
    protected List<? extends GenerationInfo> generateMemberPrototypes(PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
        List<GenerationInfo> result = new ArrayList<>();
        List<PsiField> psiFields = getPsiFields(members);
        result.add(generateBuilderInitMethod(aClass, psiFields));
        result.add(generateBuilderClass(aClass, psiFields));
        return result;
    }

    private PsiGenerationInfo<PsiMethod> generateBuilderInitMethod(PsiClass aClass, List<PsiField> psiFields) {
        String templateMacro = BuilderInitTemplatesManager.getInstance().getDefaultTemplate().getTemplate();
        String generateCode = GenerationUtil.velocityGenerateCode(aClass, psiFields, Collections.emptyMap(),
                templateMacro, 0, false);
        PsiMethod builderInitMethod = JavaPsiFacade.getElementFactory(aClass.getProject())
                .createMethodFromText(generateCode, aClass);
        return new PsiGenerationInfo<>(builderInitMethod);
    }

    private PsiGenerationInfo<PsiClass> generateBuilderClass(PsiClass aClass, List<PsiField> psiFields) {
        String templateMacro = BuilderTemplatesManager.getInstance().getDefaultTemplate().getTemplate();
        String generateCode = GenerationUtil.velocityGenerateCode(aClass, psiFields, Collections.emptyMap(),
                templateMacro, 0, false);
        PsiClass builderClass = JavaPsiFacade.getElementFactory(aClass.getProject())
                .createClassFromText(generateCode, aClass).getInnerClasses()[0];
        return new PsiGenerationInfo<>(builderClass);
    }

    private List<PsiField> getPsiFields(ClassMember[] members) {
        List<PsiField> psiFields = new ArrayList<>(members.length);
        for (ClassMember member : members) {
            if (member instanceof PsiFieldMember) {
                psiFields.add(((PsiFieldMember) member).getElement());
            }
        }
        return psiFields;
    }

    @Override
    protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember) throws IncorrectOperationException {
        return new GenerationInfo[0];
    }

    @Override
    protected String getHelpId() {
        return "Generate_Builder_Dialog";
    }

    @Override
    protected String getNothingFoundMessage() {
        return "No fields have been found to generate builder for";
    }

    @Override
    protected String getNothingAcceptedMessage() {
        return "No fields have been found to generate builder for";
    }

}
