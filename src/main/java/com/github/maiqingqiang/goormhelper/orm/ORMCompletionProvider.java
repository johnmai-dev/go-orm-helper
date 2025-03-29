package com.github.maiqingqiang.goormhelper.orm;

import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.inspections.GoTypeSpecDescriptor;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperCacheManager;
import com.github.maiqingqiang.goormhelper.ui.Icons;
import com.github.maiqingqiang.goormhelper.utils.ORMPsiTreeUtil;
import com.github.maiqingqiang.goormhelper.utils.Strings;
import com.goide.documentation.GoDocumentationProvider;
import com.goide.inspections.core.GoCallableDescriptor;
import com.goide.inspections.core.GoCallableDescriptorSet;
import com.goide.psi.*;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

public abstract class ORMCompletionProvider extends CompletionProvider<CompletionParameters> {
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        PsiElement currentElement = parameters.getPosition();

        Project project = currentElement.getProject();

        GoCallExpr goCallExpr = (GoCallExpr) PsiTreeUtil.findFirstParent(currentElement, e -> e instanceof GoCallExpr);

        if (goCallExpr == null) return;

        GoCallableDescriptor descriptor = callablesSet().find(goCallExpr, false);
        if (descriptor == null) return;

        Integer argumentIndex = callables().get(descriptor);

        if (!ORMPsiTreeUtil.callHasArgumentAtIndex(goCallExpr, argumentIndex, currentElement.getParent()) && !(currentElement.getParent().getParent() instanceof GoKey))
            return;

        String prefix = result.getPrefixMatcher().getPrefix();
        int lastSpace = prefix.lastIndexOf(' ');
        if (lastSpace >= 0 && lastSpace < prefix.length() - 1) {
            String previous = prefix.substring(0, lastSpace);
            prefix = prefix.substring(lastSpace + 1);

            result = result.withPrefixMatcher(prefix);

            if (Strings.endsWithIgnoreCaseAny(previous, Types.USE_LOGICAL_OPERATOR_SCENE.toArray(new CharSequence[]{}))) {
                for (String s : Types.LOGICAL_OPERATOR_EXPR) {
                    if (StringUtils.containsIgnoreCase(s, prefix)) {
                        result.addElement(LookupElementBuilder
                                .create(s)
                                .withPresentableText(s)
                                .withIcon(getIcon()));

                        String lowerCase = s.toLowerCase();

                        result.addElement(LookupElementBuilder
                                .create(lowerCase)
                                .withPresentableText(lowerCase)
                                .withIcon(getIcon()));
                    }
                }
                return;
            }
        }

        GoCompositeElement argument = findTargetGoCompositeElement(currentElement);

        argument = findAgainArgument(argument, parameters, descriptor, result);

        GoORMHelperCacheManager manager = GoORMHelperCacheManager.getInstance(project);

        if (argument instanceof GoStatement) {
            String comment = GoDocumentationProvider.getCommentText(GoDocumentationProvider.getCommentsForElement(argument), false);
            Matcher matcher;
            String schema = null;

            if ((matcher = Types.MODEL_ANNOTATION_PATTERN.matcher(comment)).find()) {
                schema = matcher.group(1);
            }

            if ((matcher = Types.TABLE_ANNOTATION_PATTERN.matcher(comment)).find()) {
                String table = matcher.group(1);
                schema = manager.getTableStructMapping().get(table);
            }

            handleSchema(parameters, result, project, descriptor, schema);
        } else if (argument instanceof GoExpression) {
            if (argument instanceof GoUnaryExpr goUnaryExpr) {
                if (goUnaryExpr.getExpression() instanceof GoCompositeLit goCompositeLit) {
                    handleGoTypeReferenceExpression(parameters, result, descriptor, goCompositeLit.getTypeReferenceExpression());
                } else if (goUnaryExpr.getExpression() instanceof GoReferenceExpression goReferenceExpression) {
                    if (goReferenceExpression.resolve() instanceof GoVarDefinition goVarDefinition) {
                        GoType goType = goVarDefinition.getGoType(ResolveState.initial());
                        handleGoType(parameters, result, descriptor, goType);
                    } else if (goReferenceExpression.resolve() instanceof GoParamDefinition goParamDefinition) {
                        GoType goType = goParamDefinition.getGoTypeInner(ResolveState.initial());
                        handleGoType(parameters, result, descriptor, goType);
                    }
                }
            } else if (argument instanceof GoBuiltinCallExpr goBuiltinCallExpr) {
                handleGoType(parameters, result, descriptor, PsiTreeUtil.findChildOfType(goBuiltinCallExpr, GoType.class));
            } else if (argument instanceof GoReferenceExpression goReferenceExpression) {
                if (goReferenceExpression.resolve() instanceof GoVarDefinition goVarDefinition) {
                    GoType goType = PsiTreeUtil.findChildOfAnyType(goVarDefinition.getParent(), GoType.class);

                    if (goType != null) {
                        handleGoType(parameters, result, descriptor, goType);
                    } else {
                        GoCompositeLit goCompositeLit = PsiTreeUtil.findChildOfType(goVarDefinition.getParent(), GoCompositeLit.class);
                        if (goCompositeLit != null) {
                            handleGoTypeReferenceExpression(parameters, result, descriptor, goCompositeLit.getTypeReferenceExpression());
                        }
                    }
                } else if (goReferenceExpression.resolve() instanceof GoParamDefinition goParamDefinition) {
                    GoType goType = goParamDefinition.getGoTypeInner(ResolveState.initial());
                    handleGoType(parameters, result, descriptor, goType);
                }
            } else if (argument instanceof GoCompositeLit goCompositeLit) {
                handleGoTypeReferenceExpression(parameters, result, descriptor, goCompositeLit.getTypeReferenceExpression());
            } else if (argument instanceof GoStringLiteral goStringLiteral) {
                String schema = manager.getTableStructMapping().get(goStringLiteral.getDecodedText());
                handleSchema(parameters, result, project, descriptor, schema);
            }
        } else if (argument instanceof GoType goType) {
            handleGoType(parameters, result, descriptor, goType);
        }
    }

    protected GoCompositeElement findAgainArgument(GoCompositeElement argument, @NotNull CompletionParameters parameters, GoCallableDescriptor descriptor, @NotNull CompletionResultSet result) {
        return argument;
    }

    private void handleGoTypeReferenceExpression(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result, GoCallableDescriptor descriptor, GoTypeReferenceExpression expression) {
        if (expression != null && expression.resolve() instanceof GoTypeSpec goTypeSpec) {
            scanFields(parameters, descriptor, result, goTypeSpec);
        }
    }

    private void handleGoType(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result, GoCallableDescriptor descriptor, GoType goType) {
        if (goType != null) {
            goType = ORMPsiTreeUtil.getReallyGoType(goType);

            if (goType.resolve(ResolveState.initial()) instanceof GoTypeSpec goTypeSpec) {
                scanFields(parameters, descriptor, result, goTypeSpec);

            }
        }
    }

    private void handleSchema(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result, Project project, GoCallableDescriptor descriptor, String schema) {
        if (schema != null && !schema.isEmpty()) {
            Set<String> pathList = GoORMHelperCacheManager.getInstance(project).getSchemaMapping().get(schema);

            if (pathList != null) {
                for (String path : pathList) {

                    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(path);
                    if (file == null) continue;

                    GoFile goFile = (GoFile) PsiManager.getInstance(project).findFile(file);
                    if (goFile == null) continue;

                    for (GoTypeSpec goTypeSpec : PsiTreeUtil.findChildrenOfType(goFile, GoTypeSpec.class)) {
                        if (!Objects.equals(goTypeSpec.getName(), schema)) continue;

                        scanFields(parameters, descriptor, result, goTypeSpec);
                    }
                }
            }
        }
    }

    private GoCompositeElement findTargetGoCompositeElement(@NotNull PsiElement currentElement) {
        GoCompositeElement element = findGoCompositeElement(currentElement, schemaCallablesSet(), schemaCallables());

        if (element == null)
            element = findGoCompositeElement(currentElement, otherSchemaCallablesSet(), otherSchemaCallables());

        if (element == null) {
            GoCallExpr goCallExpr = (GoCallExpr) PsiTreeUtil.findFirstParent(currentElement, e -> e instanceof GoCallExpr);
            if (goCallExpr != null) {
                GoReferenceExpression goReferenceExpression = ORMPsiTreeUtil.findLastChildOfType(goCallExpr, GoReferenceExpression.class);
                if (goReferenceExpression != null && goReferenceExpression.resolve() != null) {
                    GoType goType = goReferenceExpression.getGoType(ResolveState.initial());

                    if (goType != null && goType.resolve(ResolveState.initial()) instanceof GoTypeSpec goTypeSpec) {

                        GoTypeSpecDescriptor descriptor = GoTypeSpecDescriptor.of(goTypeSpec, goType, true);

                        if (descriptor != null && allowTypes().contains(descriptor)) {
                            PsiElement resolved = goReferenceExpression.resolve();
                            if (resolved != null) {
                                element = findTargetGoCompositeElement(resolved);
                            }
                        }
                    }
                }
            }
        }

        return element;
    }

    private @Nullable GoCompositeElement findGoCompositeElementByStatement(GoStatement statement, GoCallableDescriptorSet callablesSet, Map<GoCallableDescriptor, Integer> callables) {

        String comment = GoDocumentationProvider.getCommentText(GoDocumentationProvider.getCommentsForElement(statement), false);

        if ((Types.MODEL_ANNOTATION_PATTERN.matcher(comment)).find()) {
            return statement;
        }

        if ((Types.TABLE_ANNOTATION_PATTERN.matcher(comment)).find()) {
            return statement;
        }

        for (GoCallExpr goCallExpr : PsiTreeUtil.findChildrenOfType(statement, GoCallExpr.class)) {

            GoCompositeElement element = customFindGoCompositeElementByGoCallExpr(goCallExpr);
            if (element != null) return element;

            GoCallableDescriptor descriptor = callablesSet.find(goCallExpr, false);
            if (descriptor == null) continue;

            Integer argumentIndex = callables.get(descriptor);

            List<GoExpression> expressionList = goCallExpr.getArgumentList().getExpressionList();
            if (expressionList.size() >= argumentIndex + 1) return expressionList.get(argumentIndex);
        }

        return null;
    }

    protected GoCompositeElement customFindGoCompositeElementByGoCallExpr(GoCallExpr goCallExpr) {
        return null;
    }

    private @Nullable GoCompositeElement findGoCompositeElement(@NotNull PsiElement currentElement, GoCallableDescriptorSet callablesSet, Map<GoCallableDescriptor, Integer> callables) {
        if (callables == null || callablesSet == null) return null;

        GoStatement currentStatement = (GoStatement) PsiTreeUtil.findFirstParent(currentElement, e -> e instanceof GoStatement);

        GoCompositeElement argument = findGoCompositeElementByStatement(currentStatement, callablesSet, callables);
        if (argument != null) return argument;

        if (currentStatement instanceof GoAssignmentStatement goAssignmentStatement) {
            for (GoExpression goExpression : goAssignmentStatement.getLeftHandExprList().getExpressionList()) {

                if (checkAllowTypeByGoPointerType(goExpression.getGoType(ResolveState.initial())) && goExpression.getReference() != null) {
                    PsiElement resolved = goExpression.getReference().resolve();
                    if (resolved != null) {
                        argument = findGoCompositeElement(resolved, callablesSet, callables);
                        if (argument != null) return argument;
                    }
                }
            }
        } else if (currentStatement instanceof GoSimpleStatement goSimpleStatement) {
            if (goSimpleStatement.getShortVarDeclaration() != null) {
                GoShortVarDeclaration shortVarDeclaration = goSimpleStatement.getShortVarDeclaration();

                for (GoVarDefinition goVarDefinition : shortVarDeclaration.getDefinitionList()) {
                    if (checkAllowType(goVarDefinition)) {
                        for (PsiReference search : GoReferencesSearch.search(shortVarDeclaration.getDefinitionList().get(0))) {
                            GoStatement statement = (GoStatement) PsiTreeUtil.findFirstParent(search.getElement(), e -> e instanceof GoStatement);

                            argument = findGoCompositeElementByStatement(statement, callablesSet, callables);
                            if (argument != null) return argument;
                        }
                    }
                }
            } else {
                GoReferenceExpression expression = ORMPsiTreeUtil.findLastChildOfType(goSimpleStatement, GoReferenceExpression.class);
                if (expression != null && expression.resolve() instanceof GoVarDefinition goVarDefinition) {
                    if (checkAllowType(goVarDefinition)) {
                        return findGoCompositeElement(goVarDefinition, callablesSet, callables);
                    }
                }
            }
        }

        return null;
    }


    private boolean checkAllowType(GoVarDefinition goVarDefinition) {
        GoType goType = goVarDefinition.getGoType(ResolveState.initial());
        return checkAllowTypeByGoPointerType(goType);
    }

    public boolean checkAllowTypeByGoPointerType(GoType goType) {
        if (goType instanceof GoPointerType goPointerType) {
            goType = goPointerType.getType();
        }

        if (goType != null && goType.resolve(ResolveState.initial()) instanceof GoTypeSpec goTypeSpec) {
            GoTypeSpecDescriptor descriptor = GoTypeSpecDescriptor.of(goTypeSpec, goType, true);
            return descriptor != null && allowTypes().contains(descriptor);
        }

        return false;
    }

    protected boolean checkGoFieldDeclaration(GoFieldDeclaration field) {
        return true;
    }

    private void scanFields(@NotNull CompletionParameters parameters, GoCallableDescriptor descriptor, @NotNull CompletionResultSet result, @NotNull GoTypeSpec goTypeSpec) {
        if (goTypeSpec.getSpecType().getType() instanceof GoStructType goStructType) {
            for (GoFieldDeclaration field : goStructType.getFieldDeclarationList()) {

                if (!checkGoFieldDeclaration(field)) continue;

                String column = getColumn(field);
                String comment = getComment(field);
                String type = "";

                if (field.getType() != null) {
                    type = field.getType().getPresentationText();
                }

                if (column != null && column.isEmpty()) {
                    if (field.getFieldDefinitionList().isEmpty() && field.getAnonymousFieldDefinition() != null) {
                        GoType goType = field.getAnonymousFieldDefinition().getGoType(ResolveState.initial());
                        if (goType == null) continue;

                        GoTypeSpec spec = (GoTypeSpec) goType.resolve(ResolveState.initial());
                        if (spec == null) continue;

                        scanFields(parameters, descriptor, result, spec);
                        continue;
                    }
                    if (field.getFieldDefinitionList().isEmpty()) continue;
                    String name = field.getFieldDefinitionList().getFirst().getName();

                    if (name != null) {
                        column = Strings.toSnakeCase(name);
                    }
                }


                if (comment != null && comment.isEmpty()) {
                    comment = GoDocumentationProvider.getCommentText(GoDocumentationProvider.getCommentsForElement(field), false);
                }

                if (column != null && !column.contains(result.getPrefixMatcher().getPrefix())) continue;

                addElement(result, column, comment, type, goTypeSpec);

                if (!(parameters.getPosition().getParent().getParent() instanceof GoKey)) {
                    Map<GoCallableDescriptor, List<String>> queryExpr = queryExpr();
                    if (queryExpr != null) {
                        List<String> whereExpr = queryExpr.get(descriptor);
                        if (whereExpr != null) {
                            for (String s : whereExpr) {
                                addElement(result, String.format(s, column), comment, type, goTypeSpec);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void addElement(@NotNull CompletionResultSet result, String column, String comment, String type, @NotNull GoTypeSpec goTypeSpec) {
        LookupElementBuilder builder = LookupElementBuilder
                .createWithSmartPointer(column, goTypeSpec)
                .withPresentableText(column)
                .withTypeText(type)
                .withIcon(getIcon())
                .withTailText(" " + comment, true);

        if (goTypeSpec.getContainingFile().getVirtualFile() != null) {
            String path = goTypeSpec.getContainingFile().getVirtualFile().getCanonicalPath();
            if (path != null) {
                String basePath = goTypeSpec.getProject().getBasePath();
                if (basePath != null) {
                    builder = builder.appendTailText(" " + path.replace(basePath + "/", ""), true);
                }
            }
        }

        result.addElement(builder);
    }

    protected Icon getIcon() {
        return Icons.Icon12x12;
    }

    public abstract Map<GoCallableDescriptor, Integer> callables();

    public abstract GoCallableDescriptorSet callablesSet();

    public abstract @Nullable Map<GoCallableDescriptor, Integer> schemaCallables();

    public abstract @Nullable GoCallableDescriptorSet schemaCallablesSet();

    public abstract @Nullable Map<GoCallableDescriptor, Integer> otherSchemaCallables();

    public abstract @Nullable GoCallableDescriptorSet otherSchemaCallablesSet();

    public abstract @Nullable Map<GoCallableDescriptor, List<String>> queryExpr();

    public abstract @Nullable String getColumn(@NotNull GoFieldDeclaration field);

    public abstract @Nullable String getComment(@NotNull GoFieldDeclaration field);

    public abstract @NotNull Set<GoCallableDescriptor> allowTypes();
}
