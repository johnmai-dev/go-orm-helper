package com.github.maiqingqiang.goormhelper.actions;

import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperProjectSettings;
import com.github.maiqingqiang.goormhelper.sql2struct.ISQL2Struct;
import com.github.maiqingqiang.goormhelper.ui.ConvertSettingDialogWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SQL2StructAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();

        GoORMHelperProjectSettings.State state = Objects.requireNonNull(GoORMHelperProjectSettings.getInstance(Objects.requireNonNull(project)).getState());

        Types.ORM selectedORM = state.defaultORM;
        Types.Database selectedDatabase = state.defaultDatabase;

        if (selectedORM == Types.ORM.AskEveryTime || selectedDatabase == Types.Database.AskEveryTime) {
            ConvertSettingDialogWrapper wrapper = new ConvertSettingDialogWrapper(project);
            if (!wrapper.showAndGet()) return;

            selectedORM = (Types.ORM) wrapper.getOrmComponent().getComponent().getSelectedItem();
            selectedDatabase = (Types.Database) wrapper.getDatabaseComponent().getComponent().getSelectedItem();
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        String text = editor.getSelectionModel().getSelectedText();

        Caret currentCaret = editor.getCaretModel().getCurrentCaret();
        int start = currentCaret.getSelectionStart();
        int end = currentCaret.getSelectionEnd();

        final Types.ORM finalSelectedORM = selectedORM;
        final Types.Database finalSelectedDatabase = selectedDatabase;

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (text == null || text.isEmpty() || finalSelectedORM == null || finalSelectedDatabase == null) return;

            ISQL2Struct sql2Struct = finalSelectedORM.sql2Struct(project, text, finalSelectedDatabase.toDbType());

            if (sql2Struct != null) {
                editor.getDocument().replaceString(start, end, sql2Struct.convert());
            }
        });
    }
}
