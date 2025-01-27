package com.github.maiqingqiang.goormhelper.ui;

import com.github.maiqingqiang.goormhelper.GoORMHelperBundle;
import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperCacheManager;
import com.github.maiqingqiang.goormhelper.services.GoORMHelperProjectSettings;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurableUi;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GoORMHelperSettingForm implements ConfigurableUi<GoORMHelperProjectSettings> {

    private final Project project;
    private JPanel panel;
    private ComboBox<Types.ORM> ormComboBox;
    private ComboBox<Types.Database> databaseComboBox;
    private ComboBox<Types.TagMode> tagModeComboBox;

    private JCheckBox enableGlobalScanCheckBox;
    private ListTableModel<String> scanPathListTableModel;
    //    private TextFieldWithBrowseButton sqlPathTextField;
    private TableView<String> scanPathTableView;

    private JCheckBox findStructTableNameFuncCheckBox;

    public GoORMHelperSettingForm(Project project) {
        this.project = project;
        initComponent();
    }

    private static FileChooserDescriptor getFileChooserDescriptor(String title) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                true,
                true,
                false,
                false,
                false,
                false
        )
                .withShowFileSystemRoots(true)
                .withShowHiddenFiles(true);

        if (title != null) {
            descriptor.setTitle(title);
        }

        return descriptor;
    }

    private void initComponent() {
        ormComboBox = new ComboBox<>(Types.ORM.values());
        databaseComboBox = new ComboBox<>(Types.Database.values());
        tagModeComboBox = new ComboBox<>(Types.TagMode.values());

        enableGlobalScanCheckBox = new JCheckBox(GoORMHelperBundle.message("setting.enableGlobalScanCheckBox.title"));
        findStructTableNameFuncCheckBox = new JCheckBox(GoORMHelperBundle.message("setting.findStructTableNameFuncCheckBox.title"));

        panel = FormBuilder
                .createFormBuilder()
                .addLabeledComponent(GoORMHelperBundle.message("setting.ormComboBox.title"), ormComboBox)
                .addLabeledComponent(GoORMHelperBundle.message("setting.databaseComboBox.title"), databaseComboBox)
                .addLabeledComponent(GoORMHelperBundle.message("setting.tagModeComboBox.title"), tagModeComboBox)
                .addComponent(enableGlobalScanCheckBox)
                .addComponentFillVertically(initScanPathComponent(), 0)
                .addComponent(new JSeparator())
                .addComponent(new JLabel(GoORMHelperBundle.message("setting.experimental.title")))
                .addComponent(findStructTableNameFuncCheckBox)
                .getPanel();


        enableGlobalScanCheckBox.addChangeListener(e -> scanPathTableView.setEnabled(!enableGlobalScanCheckBox.isSelected()));
    }

    private @NotNull JPanel initScanPathComponent() {
        scanPathListTableModel = new ListTableModel<>(new LocationColumn(GoORMHelperBundle.message("setting.tableview.column.location")));
        scanPathTableView = new TableView<>(scanPathListTableModel);
        scanPathTableView.setStriped(true);
        scanPathTableView.setMinRowHeight(25);
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(scanPathTableView, null);

        decorator.setAddAction(button -> {
            VirtualFile selectedFile = FileChooser.chooseFile(getFileChooserDescriptor(GoORMHelperBundle.message("setting.decorator.title")), project, null);

            if (selectedFile != null) {
                scanPathListTableModel.insertRow(0, selectedFile.getUrl());
            }
        });

        decorator.setRemoveAction(button -> {
            int[] selectedRows = scanPathTableView.getSelectedRows();
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                scanPathListTableModel.removeRow(selectedRows[i]);
            }
        });

        decorator.setMoveDownAction(button -> {
            int[] selectedRows = scanPathTableView.getSelectedRows();
            for (int selectedRow : selectedRows) {
                scanPathListTableModel.exchangeRows(selectedRow, selectedRow + 1);
            }
        });

        decorator.setMoveUpAction(button -> {
            int[] selectedRows = scanPathTableView.getSelectedRows();
            for (int selectedRow : selectedRows) {
                scanPathListTableModel.exchangeRows(selectedRow, selectedRow - 1);
            }
        });

        JPanel scanPathPanel = decorator.createPanel();

        UIUtil.addBorder(scanPathPanel, IdeBorderFactory.createTitledBorder(GoORMHelperBundle.message("setting.decorator.title"), false));

        return scanPathPanel;
    }

    @Override
    public void reset(@NotNull GoORMHelperProjectSettings settings) {
        loadSettings(settings);
    }

    private void loadSettings(@NotNull GoORMHelperProjectSettings settings) {
        GoORMHelperProjectSettings.State state = Objects.requireNonNull(settings.getState());

        ormComboBox.setSelectedItem(state.defaultORM);
        databaseComboBox.setSelectedItem(state.defaultDatabase);
        tagModeComboBox.setSelectedItem(state.defaultTagMode);

//        sqlPathTextField.setText(state.sqlPath);
        enableGlobalScanCheckBox.setSelected(state.enableGlobalScan);
        findStructTableNameFuncCheckBox.setSelected(state.findStructTableNameFunc);
        scanPathListTableModel.setItems(new ArrayList<>(state.scanPathList));
        scanPathTableView.setEnabled(!state.enableGlobalScan);
    }

    @Override
    public boolean isModified(@NotNull GoORMHelperProjectSettings settings) {
        GoORMHelperProjectSettings.State state = Objects.requireNonNull(settings.getState());

        return !(ormComboBox.getSelectedItem() == state.defaultORM
                && databaseComboBox.getSelectedItem() == state.defaultDatabase
                && tagModeComboBox.getSelectedItem() == state.defaultTagMode
//                && sqlPathTextField.getText().equals(state.sqlPath)
                && enableGlobalScanCheckBox.isSelected() == state.enableGlobalScan
                && findStructTableNameFuncCheckBox.isSelected() == state.findStructTableNameFunc
                && scanPathTableView.getItems().equals(state.scanPathList));
    }

    @Override
    public void apply(@NotNull GoORMHelperProjectSettings settings) {
        boolean oldEnableGlobalScan = Objects.requireNonNull(settings.getState()).enableGlobalScan;
        boolean oldFindStructTableNameFunc = Objects.requireNonNull(settings.getState()).findStructTableNameFunc;
        List<String> oldscanPathList = scanPathListTableModel.getItems();

        settings.setDefaultDatabase((Types.Database) databaseComboBox.getSelectedItem());
        settings.setDefaultTagMode((Types.TagMode) tagModeComboBox.getSelectedItem());
        settings.setDefaultORM((Types.ORM) ormComboBox.getSelectedItem());
        settings.setEnableGlobalScan(enableGlobalScanCheckBox.isSelected());
        settings.setFindStructTableNameFunc(findStructTableNameFuncCheckBox.isSelected());
        settings.setScanPathList(scanPathListTableModel.getItems());
//        settings.setSQLPath(sqlPathTextField.getText());

        if (oldEnableGlobalScan != enableGlobalScanCheckBox.isSelected()
                || !oldscanPathList.equals(scanPathListTableModel.getItems())
                || oldFindStructTableNameFunc != findStructTableNameFuncCheckBox.isSelected()) {
            GoORMHelperCacheManager manager = GoORMHelperCacheManager.getInstance(project);
            manager.reset();
            manager.scan();
        }
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

}
