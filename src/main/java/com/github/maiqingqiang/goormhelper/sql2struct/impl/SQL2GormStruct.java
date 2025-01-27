package com.github.maiqingqiang.goormhelper.sql2struct.impl;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperProjectSettings;
import com.github.maiqingqiang.goormhelper.utils.Strings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SQL2GormStruct extends SQL2Struct {
    public SQL2GormStruct(Project project, String sql, DbType dbType) {
        super(project, sql, dbType);
    }

    @Override
    protected void generateORMTag(@NotNull StringBuilder stringBuilder, @NotNull SQLColumnDefinition definition) {
        GoORMHelperProjectSettings.State state = Objects.requireNonNull(GoORMHelperProjectSettings.getInstance(project).getState());

        if (state.defaultTagMode == Types.TagMode.Compact || state.defaultTagMode == Types.TagMode.Full) {
            stringBuilder.append("gorm:\"")
                         .append("column:").append(getColumn(definition)).append(";");
        }

        if (state.defaultTagMode == Types.TagMode.Full) {
            appendOriginalTagAttributes(stringBuilder, definition);
        }

        stringBuilder.append("\" ");
    }

    private void appendOriginalTagAttributes(@NotNull StringBuilder stringBuilder, @NotNull SQLColumnDefinition definition) {
        stringBuilder.append("type:").append(getDBType(definition)).append(";");

        String comment = getComment(definition);
        if (!comment.isEmpty()) {
            stringBuilder.append("comment:").append(comment).append(";");
        }

        if (definition.isPrimaryKey()) {
            stringBuilder.append("primaryKey;");
        }

        if (definition.containsNotNullConstraint()) {
            stringBuilder.append("not null;");
        }

        if (definition.getDefaultExpr() != null) {
            String def = Strings.clearSingleQuotn(definition.getDefaultExpr().toString());
            if (!def.isEmpty()) {
                stringBuilder.append("default:").append(def).append(";");
            }
        }
    }
}