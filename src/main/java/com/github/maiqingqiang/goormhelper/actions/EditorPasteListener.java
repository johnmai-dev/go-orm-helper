package com.github.maiqingqiang.goormhelper.actions;

import com.github.maiqingqiang.goormhelper.GoORMHelperBundle;
import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperProjectSettings;
import com.github.maiqingqiang.goormhelper.sql2struct.ISQL2Struct;
import com.github.maiqingqiang.goormhelper.ui.ConvertSettingDialogWrapper;
import com.goide.psi.GoFile;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.ValidationError;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.util.Collections;
import java.util.List;

public class EditorPasteListener extends EditorActionHandler {

    private final EditorActionHandler handler;

    public EditorPasteListener(EditorActionHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        PsiFile file = CommonDataKeys.PSI_FILE.getData(dataContext);
        if (!(file instanceof GoFile)) {
            handler.execute(editor, caret, dataContext);
            return;
        }

        String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        if (!verifySQL(text)) {
            handler.execute(editor, caret, dataContext);
            return;
        }

        Project project = editor.getProject();
        if (project == null) {
            handler.execute(editor, caret, dataContext);
            return;
        }

        GoORMHelperProjectSettings.State state = GoORMHelperProjectSettings.getInstance(project).getState();
        if (state == null) {
            handler.execute(editor, caret, dataContext);
            return;
        }

        Types.ORM selectedORM = state.defaultORM;
        Types.Database selectedDatabase = state.defaultDatabase;

        if (selectedORM == Types.ORM.AskEveryTime || selectedDatabase == Types.Database.AskEveryTime) {
            ConvertSettingDialogWrapper wrapper = new ConvertSettingDialogWrapper(project);
            if (!wrapper.showAndGet()) {
                handler.execute(editor, caret, dataContext);
                return;
            }
            selectedORM = (Types.ORM) wrapper.getOrmComponent().getComponent().getSelectedItem();
            selectedDatabase = (Types.Database) wrapper.getDatabaseComponent().getComponent().getSelectedItem();
        }

        final Types.ORM finalSelectedORM = selectedORM;
        final Types.Database finalSelectedDatabase = selectedDatabase;

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (text == null || text.isEmpty() || finalSelectedORM == null || finalSelectedDatabase == null) {
                return;
            }

            ISQL2Struct sql2Struct = finalSelectedORM.sql2Struct(project, text, finalSelectedDatabase.toDbType());
            if (sql2Struct == null) {
                return;
            }

            String struct = sql2Struct.convert();
            Caret currentCaret = editor.getCaretModel().getCurrentCaret();
            int start = currentCaret.getSelectionStart();
            int end = currentCaret.getSelectionEnd();
            Document document = editor.getDocument();

            try {
                if (start == end) {
                    document.insertString(start, struct);
                } else {
                    document.replaceString(start, end, struct);
                }
                currentCaret.moveToOffset(start + struct.length());
            } catch (Exception ignored) {
                Notifications.Bus.notify(
                        new Notification(
                                GoORMHelperBundle.message("name"),
                                GoORMHelperBundle.message("sql.convert.struct.not.support"),
                                GoORMHelperBundle.message("sql.convert.struct.check"),
                                NotificationType.WARNING),
                        project
                );
                handler.execute(editor, caret, dataContext);
            }
        });
    }

    private boolean verifySQL(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            Validation validation = new Validation(Collections.singletonList(FeaturesAllowed.CREATE), sql);
            List<ValidationError> errors = validation.validate();
            return errors.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}