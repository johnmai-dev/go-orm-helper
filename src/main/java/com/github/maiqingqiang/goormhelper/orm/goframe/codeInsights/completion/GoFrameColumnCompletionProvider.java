package com.github.maiqingqiang.goormhelper.orm.goframe.codeInsights.completion;

import com.github.maiqingqiang.goormhelper.inspections.GoTypeSpecDescriptor;
import com.github.maiqingqiang.goormhelper.orm.ORMCompletionProvider;
import com.github.maiqingqiang.goormhelper.orm.goframe.GoFrameTypes;
import com.github.maiqingqiang.goormhelper.ui.Icons;
import com.github.maiqingqiang.goormhelper.utils.Strings;
import com.goide.documentation.GoDocumentationProvider;
import com.goide.inspections.core.GoCallableDescriptor;
import com.goide.inspections.core.GoCallableDescriptorSet;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class GoFrameColumnCompletionProvider extends ORMCompletionProvider {

    @Contract(pure = true)
    private static String @NotNull [] parseTag(@NotNull String tagStr) {
        return tagStr.split(",");
    }

    @Override
    public Map<GoCallableDescriptor, Integer> callables() {
        return GoFrameTypes.CALLABLES;
    }

    @Override
    public GoCallableDescriptorSet callablesSet() {
        return GoFrameTypes.CALLABLES_SET;
    }

    @Override
    public Map<GoCallableDescriptor, Integer> schemaCallables() {
        return GoFrameTypes.SCHEMA_CALLABLES;
    }

    @Override
    public GoCallableDescriptorSet schemaCallablesSet() {
        return GoFrameTypes.SCHEMA_CALLABLES_SET;
    }

    @Override
    public @Nullable Map<GoCallableDescriptor, Integer> otherSchemaCallables() {
        return null;
    }

    @Override
    public @Nullable GoCallableDescriptorSet otherSchemaCallablesSet() {
        return null;
    }

    @Override
    public Map<GoCallableDescriptor, List<String>> queryExpr() {
        return GoFrameTypes.QUERY_EXPR;
    }

    @Override
    public String getColumn(@NotNull GoFieldDeclaration field) {
        String column = "";
        GoTag tag = field.getTag();
        if (tag != null && tag.getValue("orm") != null) {
            String[] vals = parseTag(Objects.requireNonNull(tag.getValue("orm")));
            if (vals.length > 0) {
                column = vals[0];
            }
        }
        return column;
    }

    @Override
    public String getComment(@NotNull GoFieldDeclaration field) {
        return "";
    }

    @Override
    public @NotNull Set<GoCallableDescriptor> allowTypes() {
        return GoFrameTypes.ALLOW_TYPES;
    }

    @Override
    protected Icon getIcon() {
        return Icons.GoFrame18x12;
    }

    @Override
    protected boolean checkGoFieldDeclaration(GoFieldDeclaration field) {
        if (field.getAnonymousFieldDefinition() == null) return true;

        GoType goType = field.getAnonymousFieldDefinition().getType();

        GoTypeSpec goTypeSpec = (GoTypeSpec) goType.resolve(ResolveState.initial());

        if (goTypeSpec == null) return true;

        GoTypeSpecDescriptor descriptor = GoTypeSpecDescriptor.of(goTypeSpec, goType, true);

        if (descriptor == null) return true;

        return !descriptor.equals(GoFrameTypes.G_META) && !descriptor.equals(GoFrameTypes.GMETA_META);
    }

    @Override
    protected GoCompositeElement customFindGoCompositeElementByGoCallExpr(GoCallExpr goCallExpr) {
        return findGoTypeByReceiver(goCallExpr, allowTypes());
    }

    private @Nullable GoType findGoTypeByReceiver(GoCallExpr goCallExpr, Set<GoCallableDescriptor> allowTypes) {
        GoReferenceExpression callRef = GoPsiUtil.getCallReference(goCallExpr);
        String refName = callRef != null ? callRef.getIdentifier().getText() : null;

        if (callRef != null && GoFrameTypes.OTHER_SCHEMA_CALLABLES.contains(refName)) {
            GoMethodDeclaration declaration = ObjectUtils.tryCast(callRef.resolve(), GoMethodDeclaration.class);
            if (declaration != null) {
                GoType resultType = declaration.getResultType();

                if (resultType instanceof GoPointerType goPointerType) {
                    resultType = goPointerType.getType();
                }

                if (resultType != null && resultType.resolve(ResolveState.initial()) instanceof GoTypeSpec goTypeSpec) {
                    GoTypeSpecDescriptor descriptor = GoTypeSpecDescriptor.of(goTypeSpec, resultType, true);

                    if (allowTypes != null && allowTypes.contains(descriptor) && declaration.getReceiver() != null && declaration.getReceiver().getType() instanceof GoPointerType goPointerType) {
                        GoType goType = goPointerType.getType();
                        if (goType != null && goType.resolve(ResolveState.initial()) instanceof GoTypeSpec spec &&
                                spec.getSpecType().getType() instanceof GoStructType goStructType) {

                            for (GoFieldDeclaration field : goStructType.getFieldDeclarationList()) {
                                if (field.getFieldDefinitionList().isEmpty()) continue;
                                if (GoFrameTypes.ALLOW_FIELDS.contains(field.getFieldDefinitionList().getFirst().getName()) && field.getType() != null) {
                                    return field.getType();
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected GoCompositeElement findAgainArgument(GoCompositeElement argument, @NotNull CompletionParameters parameters, GoCallableDescriptor descriptor, @NotNull CompletionResultSet result) {
        if (argument instanceof GoCallExpr goCallExpr) {
            GoType goType = findGoTypeByReceiver(goCallExpr, Set.of(GoTypeSpecDescriptor.of("builtin.string")));
            if (handleGoTypeSpec(goType, parameters, descriptor, result)) {
                return null;
            }
        } else if (argument instanceof GoType goType && handleGoTypeSpec(goType, parameters, descriptor, result)) {
            return null;
        }
        return argument;
    }

    private boolean handleGoTypeSpec(GoType goType, @NotNull CompletionParameters parameters, GoCallableDescriptor descriptor, @NotNull CompletionResultSet result) {
        if (goType != null && goType.resolve(ResolveState.initial()) instanceof GoTypeSpec goTypeSpec) {
            HashMap<String, String> columnMap = new HashMap<>();

            for (PsiReference search : GoReferencesSearch.search(goTypeSpec)) {
                if (search.getElement().getParent() instanceof GoCompositeLit goCompositeLit) {
                    GoLiteralValue goLiteralValue = goCompositeLit.getLiteralValue();
                    if (goLiteralValue != null) {
                        for (GoElement goElement : goLiteralValue.getElementList()) {

                            if (goElement.getKey() != null && goElement.getKey().getFieldName() != null && goElement.getValue() != null && goElement.getValue().getExpression() instanceof GoStringLiteral goStringLiteral) {
                                columnMap.put(goElement.getKey().getFieldName().getIdentifier().getText(), goStringLiteral.getDecodedText());
                            }
                        }
                    }
                }
            }

            scanFieldsByGoFrame(parameters, descriptor, result, goTypeSpec, columnMap);

            return true;
        }

        return false;
    }

    private void scanFieldsByGoFrame(@NotNull CompletionParameters parameters, GoCallableDescriptor descriptor, @NotNull CompletionResultSet result, @NotNull GoTypeSpec goTypeSpec, HashMap<String, String> columnMap) {
        if (goTypeSpec.getSpecType().getType() instanceof GoStructType goStructType) {
            for (GoFieldDeclaration field : goStructType.getFieldDeclarationList()) {

                if (!checkGoFieldDeclaration(field)) continue;
                if (field.getFieldDefinitionList().isEmpty()) continue;

                String name = field.getFieldDefinitionList().getFirst().getName();

                String column = columnMap.get(name);
                String comment = getComment(field);
                String type = "";

                if (field.getType() != null) {
                    type = field.getType().getPresentationText();
                }

                if (column != null && column.isEmpty()) {
                    column = getColumn(field);

                    if (column != null && column.isEmpty()) {
                        if (field.getFieldDefinitionList().isEmpty() && field.getAnonymousFieldDefinition() != null) {
                            GoType goType = field.getAnonymousFieldDefinition().getGoType(ResolveState.initial());
                            if (goType == null) continue;

                            GoTypeSpec spec = (GoTypeSpec) goType.resolve(ResolveState.initial());
                            if (spec == null) continue;

                            scanFieldsByGoFrame(parameters, descriptor, result, spec, columnMap);
                            continue;
                        }

                        if (name != null) {
                            column = Strings.toSnakeCase(name);
                        }
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


}
