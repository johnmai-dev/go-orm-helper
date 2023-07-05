package com.github.maiqingqiang.goormhelper.orm;

import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperManager;
import com.github.maiqingqiang.goormhelper.ui.Icons;
import com.github.maiqingqiang.goormhelper.utils.Strings;
import com.goide.documentation.GoDocumentationProvider;
import com.goide.inspections.core.GoCallableDescriptor;
import com.goide.inspections.core.GoCallableDescriptorSet;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiUtil;
import com.google.common.base.CaseFormat;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

public abstract class ORMCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static boolean hasArgumentAtIndex(@NotNull GoCallExpr call, int argumentIndex, @NotNull PsiElement argument) {
        if (argumentIndex == -1) return true;
        argument = GoPsiUtil.skipParens(argument);
        return argument == ContainerUtil.getOrElse(call.getArgumentList().getExpressionList(), argumentIndex, (Object) null);
    }

    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        Project project = parameters.getPosition().getProject();

        GoCallExpr goCallExpr = (GoCallExpr) PsiTreeUtil.findFirstParent(parameters.getPosition(), element -> element instanceof GoCallExpr);

        if (goCallExpr == null) return;

        GoCallableDescriptor descriptor = callablesSet().find(goCallExpr, false);
        if (descriptor == null) return;

        Integer argumentIndex = callables().get(descriptor);

        if (!hasArgumentAtIndex(goCallExpr, argumentIndex, parameters.getPosition().getParent()) && !(parameters.getPosition().getParent().getParent() instanceof GoKey))
            return;

        String schema = scanSchema(project, parameters.getPosition());

        if (schema == null || schema.isEmpty()) return;

        List<String> pathList = Objects.requireNonNull(GoORMHelperManager.getInstance(project).getState()).schemaMapping.get(schema);

        if (pathList == null) return;

        for (String path : pathList) {

            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(path);
            if (file == null) continue;

            GoFile goFile = (GoFile) PsiManager.getInstance(project).findFile(file);
            if (goFile == null) continue;

            for (GoTypeDeclaration goTypeDeclaration : goFile.findChildrenByClass(GoTypeDeclaration.class)) {
                GoTypeSpec goTypeSpec = goTypeDeclaration.getTypeSpecList().get(0);

                if (!Objects.equals(goTypeSpec.getName(), schema)) continue;

                scanFields(descriptor, result, goTypeSpec);
            }
        }
    }

    private void scanFields(GoCallableDescriptor descriptor, @NotNull CompletionResultSet result, @NotNull GoTypeSpec goTypeSpec) {
        if (goTypeSpec.getSpecType().getType() instanceof GoStructType goStructType) {
            for (GoFieldDeclaration field : goStructType.getFieldDeclarationList()) {
                String column = getColumn(field);
                String comment = getComment(field);
                String type = "";

                if (field.getType() != null) {
                    type = field.getType().getPresentationText();
                }

                if (column.isEmpty()) {
                    if (field.getFieldDefinitionList().size() == 0 && field.getAnonymousFieldDefinition() != null) {
                        GoType goType = field.getAnonymousFieldDefinition().getGoType(ResolveState.initial());
                        if (goType == null) continue;

                        GoTypeSpec spec = (GoTypeSpec) goType.resolve(ResolveState.initial());
                        if (spec == null) continue;

                        scanFields(descriptor, result, spec);
                        continue;
                    }

                    String name = field.getFieldDefinitionList().get(0).getName();

                    if (name != null) {
                        column = Strings.replaceCommonInitialisms(name);
                        column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, column);
                    }
                }


                if (comment.isEmpty()) {
                    comment = GoDocumentationProvider.getCommentText(GoDocumentationProvider.getCommentsForElement(field), false);
                }

                addElement(result, column, comment, type);

                List<String> whereExpr = queryExpr().get(descriptor);
                if (whereExpr != null) {
                    for (String s : whereExpr) {
                        addElement(result, String.format(s, column), comment, type);
                    }
                }
            }
        }
    }

    private @Nullable String scanSchema(Project project, @NotNull PsiElement psiElement) {

        String schema = "";

        GoStatement currentStatement = (GoStatement) PsiTreeUtil.findFirstParent(psiElement, element -> element instanceof GoStatement);

        if (currentStatement == null) return null;

        schema = findSchema(project, currentStatement);
        if (schema != null && !schema.isEmpty()) return schema;


        GoVarDefinition currentGoVarDefinition = PsiTreeUtil.findChildOfType(currentStatement, GoVarDefinition.class);
        if (currentGoVarDefinition != null) {
            schema = searchGoVarDefinitionReferences(project, currentGoVarDefinition);
            if (schema != null && !schema.isEmpty()) return schema;
        }

        GoReferenceExpression lastGoReferenceExpression = null;

        for (GoReferenceExpression goReferenceExpression : PsiTreeUtil.findChildrenOfType(currentStatement, GoReferenceExpression.class)) {
            GoType goType = goReferenceExpression.getGoType(ResolveState.initial());
            if (goType != null && goType.getPresentationText().equals("*DB")) {
                lastGoReferenceExpression = goReferenceExpression;
            }
        }

        if (lastGoReferenceExpression != null) {
            GoVarDefinition goVarDefinition = (GoVarDefinition) lastGoReferenceExpression.resolve();

            GoStatement resolveStatement = (GoStatement) PsiTreeUtil.findFirstParent(goVarDefinition, element -> element instanceof GoStatement);
            if (resolveStatement == null) return schema;
            schema = findSchema(project, resolveStatement);
            if (schema != null && !schema.isEmpty()) return schema;

            schema = searchGoVarDefinitionReferences(project, goVarDefinition);
            if (schema != null && !schema.isEmpty()) return schema;

            scanSchema(project, goVarDefinition);
        }
        return schema;
    }

    private String searchGoVarDefinitionReferences(Project project, GoVarDefinition goVarDefinition) {
        String schema = "";
        for (PsiReference psiReference : GoReferencesSearch.search(goVarDefinition)) {
            GoStatement statement = (GoStatement) PsiTreeUtil.findFirstParent(psiReference.getElement(), element -> element instanceof GoStatement);
            if (statement == null) continue;
            schema = findSchema(project, statement);
            if (schema != null && !schema.isEmpty()) return schema;

            for (GoVarDefinition varDefinition : PsiTreeUtil.findChildrenOfType(statement, GoVarDefinition.class)) {
                GoType goType = varDefinition.getGoType(ResolveState.initial());

                if (goType != null && goType.getPresentationText().equals("*DB")) {
                    return searchGoVarDefinitionReferences(project, varDefinition);
                }
            }
        }
        return schema;
    }

    private void addElement(@NotNull CompletionResultSet result, String column, String comment, String type) {
        result.addElement(LookupElementBuilder.create(column)
                .withTypeText(type)
                .withIcon(getIcon())
                .withTailText(" " + comment, true));
    }

    protected Icon getIcon() {
        return Icons.Icon12x12;
    }

    private String findSchema(Project project, @NotNull GoStatement statement) {
        String comment = GoDocumentationProvider.getCommentText(GoDocumentationProvider.getCommentsForElement(statement), false);
        Matcher matcher;

        if ((matcher = Types.MODEL_ANNOTATION_PATTERN.matcher(comment)).find()) {
            String schema = matcher.group(1);
            if (schema != null && !schema.isEmpty()) return schema;
        }

        if ((matcher = Types.TABLE_ANNOTATION_PATTERN.matcher(comment)).find()) {
            String table = matcher.group(1);
            GoORMHelperManager.State state = Objects.requireNonNull(GoORMHelperManager.getInstance(project).getState());
            String schema = state.tableStructMapping.get(table);
            if (schema != null && !schema.isEmpty()) return schema;
        }

        for (GoCallExpr goCallExpr : PsiTreeUtil.findChildrenOfType(statement, GoCallExpr.class)) {

            GoCallableDescriptorSet schemaCallablesSet = schemaCallablesSet();
            Map<GoCallableDescriptor, Integer> schemaCallables = schemaCallables();
            if (schemaCallablesSet != null && schemaCallables != null) {
                GoCallableDescriptor descriptor = schemaCallablesSet.find(goCallExpr, false);
                if (descriptor != null) {
                    Integer argumentIndex = schemaCallables.get(descriptor);

                    GoExpression argument = goCallExpr.getArgumentList().getExpressionList().get(argumentIndex);

                    if (argument instanceof GoUnaryExpr goUnaryExpr) {
                        if (goUnaryExpr.getExpression() instanceof GoCompositeLit goCompositeLit) {
                            if (goCompositeLit.getTypeReferenceExpression() == null) continue;
                            String schema = goCompositeLit.getTypeReferenceExpression().getIdentifier().getText();
                            if (schema != null && !schema.isEmpty()) return schema;

                        } else if (goUnaryExpr.getExpression() instanceof GoReferenceExpression goReferenceExpression) {
                            if (goReferenceExpression.resolve() instanceof GoVarDefinition goVarDefinition) {
                                GoType goType = goVarDefinition.getGoType(ResolveState.initial());
                                if (goType == null || goType.getTypeReferenceExpression() == null) continue;

                                String schema = goType.getTypeReferenceExpression().getIdentifier().getText();
                                if (schema != null && !schema.isEmpty()) return schema;

                            } else if (goReferenceExpression.resolve() instanceof GoParamDefinition goParamDefinition) {
                                GoPointerType goPointerType = PsiTreeUtil.findChildOfType(goParamDefinition.getParent(), GoPointerType.class);
                                GoType goType = PsiTreeUtil.findChildOfType(goPointerType, GoType.class);
                                if (goType != null) {
                                    if (goType.getTypeReferenceExpression() == null) continue;
                                    String schema = goType.getTypeReferenceExpression().getIdentifier().getText();
                                    if (schema != null && !schema.isEmpty()) return schema;
                                }
                            }
                        }
                    } else if (argument instanceof GoBuiltinCallExpr goBuiltinCallExpr) {
                        GoType goType = PsiTreeUtil.findChildOfType(goBuiltinCallExpr, GoType.class);
                        if (goType == null || goType.getTypeReferenceExpression() == null) continue;

                        String schema = goType.getTypeReferenceExpression().getIdentifier().getText();
                        if (schema != null && !schema.isEmpty()) return schema;

                    } else if (argument instanceof GoReferenceExpression goReferenceExpression && goReferenceExpression.resolve() instanceof GoVarDefinition goVarDefinition) {
                        GoType goType = PsiTreeUtil.findChildOfType(goVarDefinition.getParent(), GoType.class);
                        if (goType != null) {
                            if (goType.getTypeReferenceExpression() == null) continue;

                            String schema = goType.getTypeReferenceExpression().getIdentifier().getText();
                            if (schema != null && !schema.isEmpty()) return schema;

                        } else {
                            GoCompositeLit goCompositeLit = PsiTreeUtil.findChildOfType(goVarDefinition.getParent(), GoCompositeLit.class);
                            if (goCompositeLit == null || goCompositeLit.getTypeReferenceExpression() == null) continue;

                            String schema = goCompositeLit.getTypeReferenceExpression().getIdentifier().getText();
                            if (schema != null && !schema.isEmpty()) return schema;

                        }
                    }
                }
            }


            GoCallableDescriptorSet tableCallablesSet = tableCallablesSet();
            Map<GoCallableDescriptor, Integer> tableCallables = tableCallables();

            if (tableCallablesSet != null && tableCallables != null) {
                GoCallableDescriptor tableDescriptor = tableCallablesSet.find(goCallExpr, false);

                if (tableDescriptor != null) {
                    Integer argumentIndex = tableCallables.get(tableDescriptor);

                    GoExpression argument = goCallExpr.getArgumentList().getExpressionList().get(argumentIndex);

                    if (argument instanceof GoStringLiteral goStringLiteral) {
                        GoORMHelperManager.State state = Objects.requireNonNull(GoORMHelperManager.getInstance(project).getState());
                        String schema = state.tableStructMapping.get(goStringLiteral.getDecodedText());
                        if (schema != null && !schema.isEmpty()) return schema;
                    } else if (argument instanceof GoReferenceExpression goReferenceExpression &&
                            goReferenceExpression.resolve() instanceof GoVarDefinition goVarDefinition &&
                            goVarDefinition.getValue() != null) {
                        GoORMHelperManager.State state = Objects.requireNonNull(GoORMHelperManager.getInstance(project).getState());
                        String schema = state.tableStructMapping.get(goVarDefinition.getValue().getString());
                        if (schema != null && !schema.isEmpty()) return schema;
                    }
                }
            }
        }

        return null;
    }

    public abstract Map<GoCallableDescriptor, Integer> callables();

    public abstract GoCallableDescriptorSet callablesSet();

    public abstract @Nullable Map<GoCallableDescriptor, Integer> schemaCallables();

    public abstract @Nullable GoCallableDescriptorSet schemaCallablesSet();

    public abstract @Nullable Map<GoCallableDescriptor, Integer> tableCallables();

    public abstract @Nullable GoCallableDescriptorSet tableCallablesSet();

    public abstract @Nullable Map<GoCallableDescriptor, List<String>> queryExpr();

    public abstract @Nullable String getColumn(@NotNull GoFieldDeclaration field);

    public abstract @Nullable String getComment(@NotNull GoFieldDeclaration field);

}
