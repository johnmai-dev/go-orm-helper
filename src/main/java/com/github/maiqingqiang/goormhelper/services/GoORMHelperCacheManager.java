package com.github.maiqingqiang.goormhelper.services;

import com.github.maiqingqiang.goormhelper.GoORMHelperBundle;
import com.github.maiqingqiang.goormhelper.Types;
import com.github.maiqingqiang.goormhelper.bean.ScannedPath;
import com.github.maiqingqiang.goormhelper.inspections.GoTypeSpecDescriptor;
import com.github.maiqingqiang.goormhelper.orm.goframe.GoFrameTypes;
import com.github.maiqingqiang.goormhelper.utils.Strings;
import com.goide.GoFileType;
import com.goide.psi.*;
import com.goide.psi.impl.GoPsiImplUtil;
import com.goide.util.Value;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.serviceContainer.NonInjectable;
import com.intellij.util.Time;
import org.atteo.evo.inflector.English;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Service(Service.Level.PROJECT)
@State(name = "GoORMHelperCache", storages = @Storage(value = "GOHCache.xml"))
public final class GoORMHelperCacheManager implements PersistentStateComponent<GoORMHelperCacheManager.State> {
    private static final Logger LOG = Logger.getInstance(GoORMHelperCacheManager.class);

    public final Project project;
    private State state = new State();

    @NonInjectable
    private GoORMHelperCacheManager(@NotNull Project project) {
        this.project = project;
    }

    public static GoORMHelperCacheManager getInstance(@NotNull Project project) {
        return project.getService(GoORMHelperCacheManager.class);
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public void scan() {
        LOG.info("Scan Schema");
        ProgressManager.getInstance().run(new Task.Backgroundable(project, GoORMHelperBundle.message("initializing.title")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (isCacheExpired()) {
                    clearCache();
                }

                GoORMHelperProjectSettings.State settings = Objects.requireNonNull(GoORMHelperProjectSettings.getInstance(project).getState());

                if (settings.enableGlobalScan) {
                    scanGlobalProject();
                } else {
                    scanConfiguredPaths(settings);
                }
            }
        });
    }

    private boolean isCacheExpired() {
        return (state.lastTimeChecked + Time.DAY) <= System.currentTimeMillis();
    }

    private void clearCache() {
        state.lastTimeChecked = System.currentTimeMillis();
        state.schemaMapping.clear();
        state.scannedPathMapping.clear();
        state.tableStructMapping.clear();
        LOG.info("Clear GoORMHelperCache lastTimeChecked: " + state.lastTimeChecked);
    }

    private void scanGlobalProject() {
        final VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        if (projectDir != null && projectDir.isValid()) {
            scanProject(projectDir, Types.EXCLUDED_SCAN_LIST);
        }
    }

    private void scanConfiguredPaths(GoORMHelperProjectSettings.State settings) {
        for (String path : settings.scanPathList) {
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(path);
            if (file != null) {
                scanProject(file);
            }
        }
    }

    public void scanProject(@NotNull VirtualFile virtualFile) {
        scanProject(virtualFile, null);
    }

    public void scanProject(@NotNull VirtualFile root, List<String> excluded) {
        VfsUtilCore.iterateChildrenRecursively(root, file -> {
            if (!root.getPath().equals(file.getPath()) &&
                    ((excluded != null && excluded.contains(file.getName())) || file.getName().startsWith(".")))
                return false;

            if (file.isDirectory()) return true;

            return file.getFileType() instanceof GoFileType;
        }, fileOrDir -> {
            if (!fileOrDir.isDirectory()) {
                processGoFile(fileOrDir);
            }
            return true;
        });
    }

    private void processGoFile(VirtualFile fileOrDir) {
        ScannedPath scanned = state.scannedPathMapping.get(fileOrDir.getUrl());
        if (scanned == null || scanned.getLastModified() != fileOrDir.getTimeStamp()) {
            fileOrDir.refresh(true, true, () -> parseGoFile(fileOrDir));
        }
    }

    public void parseGoFile(@NotNull VirtualFile file) {
        try {
            ApplicationManager.getApplication().runReadAction(() -> {
                if (project.isDisposed()) return;

                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document == null) return;

                GoFile goFile = (GoFile) PsiManager.getInstance(project).findFile(file);
                if (goFile == null) return;

                processGoFileStructs(file, goFile);
            });
        } catch (IndexNotReadyException | IndexOutOfBoundsException e) {
            LOG.debug("Error parsing Go file: " + file.getPath(), e);
        }
    }

    private void processGoFileStructs(VirtualFile file, GoFile goFile) {
        List<String> structList = new ArrayList<>();
        Collection<GoTypeSpec> typeSpecs = PsiTreeUtil.findChildrenOfType(goFile, GoTypeSpec.class);

        for (GoTypeSpec typeSpec : typeSpecs) {
            String structName = typeSpec.getName();
            if (!(typeSpec.getSpecType().getType() instanceof GoStructType) || structName == null) {
                continue;
            }

            addSchemaMapping(structName, file);
            structList.add(structName);

            processTableMapping(typeSpec, structName);
        }

        addScannedPathMapping(file, structList);
    }

    private void processTableMapping(GoTypeSpec typeSpec, String structName) {
        GoORMHelperProjectSettings.State settings = Objects.requireNonNull(
                GoORMHelperProjectSettings.getInstance(project).getState());

        String tableName = settings.findStructTableNameFunc ?
                findTableName(typeSpec) : "";

        if (tableName.isEmpty()) {
            tableName = Strings.toSnakeCase(structName);

            String plural = English.plural(tableName);
            if (!plural.equals(tableName)) {
                addTableStructMapping(tableName, structName);
            }
        }

        addTableStructMapping(tableName, structName);
    }

    private void addTableStructMapping(String tableName, String structName) {
        if (!tableName.trim().isEmpty() && !structName.trim().isEmpty()) {
            this.state.tableStructMapping.put(tableName, structName);
        }
    }

    public void addSchemaMapping(String key, @NotNull VirtualFile file) {
        String fileUrl = file.getUrl();

        this.state.schemaMapping.computeIfAbsent(key, k -> new HashSet<>()).add(fileUrl);
    }

    private String findTableName(@NotNull GoTypeSpec typeSpec) {
        // 检查TableName方法
        String tableName = findTableNameFromMethod(typeSpec);
        if (!tableName.isEmpty()) {
            return tableName;
        }

        return findTableNameFromEmbeddedFields(typeSpec);
    }

    private String findTableNameFromMethod(@NotNull GoTypeSpec typeSpec) {
        for (GoMethodDeclaration method : GoPsiImplUtil.getMethods(typeSpec)) {
            if (method.getName() != null && method.getName().equals(Types.TABLE_NAME_FUNC)) {
                GoReturnStatement returnStatement = PsiTreeUtil.findChildOfType(method, GoReturnStatement.class);
                if (returnStatement == null || returnStatement.getExpressionList().isEmpty()) continue;

                Value<?> value = returnStatement.getExpressionList().getFirst().getValue();
                if (value != null && value.getString() != null && !value.getString().isEmpty()) {
                    return value.getString();
                }
            }
        }
        return "";
    }

    private String findTableNameFromEmbeddedFields(@NotNull GoTypeSpec typeSpec) {
        if (!(typeSpec.getSpecType().getType() instanceof GoStructType goStructType)) {
            return "";
        }

        for (GoFieldDeclaration field : goStructType.getFieldDeclarationList()) {
            if (field.getAnonymousFieldDefinition() == null) continue;

            GoType goType = field.getAnonymousFieldDefinition().getType();
            GoTypeSpec goTypeSpec = (GoTypeSpec) goType.resolve(ResolveState.initial());
            if (goTypeSpec == null) continue;

            GoTypeSpecDescriptor descriptor = GoTypeSpecDescriptor.of(goTypeSpec, goType, true);
            if (descriptor == null) continue;

            if (descriptor.equals(GoFrameTypes.G_META) || descriptor.equals(GoFrameTypes.GMETA_META)) {
                GoTag tag = field.getTag();
                if (tag != null) {
                    String ormText = tag.getValue("orm");
                    if (ormText != null && ormText.contains("table:")) {
                        return extractTableNameFromTag(ormText);
                    }
                }
            }
        }
        return "";
    }

    private String extractTableNameFromTag(String ormText) {
        for (String property : ormText.split(",")) {
            if (property.contains("table:")) {
                return property.replace("table:", "").trim();
            }
        }
        return "";
    }

    public void addScannedPathMapping(@NotNull VirtualFile file, List<String> structList) {
        ScannedPath scanned = new ScannedPath();
        scanned.setSchema(structList);
        scanned.setLastModified(file.getTimeStamp());
        this.state.scannedPathMapping.put(file.getUrl(), scanned);
    }

    public Map<String, ScannedPath> getScannedPathMapping() {
        return this.state.scannedPathMapping;
    }

    public Map<String, Set<String>> getSchemaMapping() {
        return this.state.schemaMapping;
    }

    public Map<String, String> getTableStructMapping() {
        return this.state.tableStructMapping;
    }

    public void reset() {
        this.state.lastTimeChecked = 0L;
    }

    public static class State {
        public final Map<String, Set<String>> schemaMapping = new HashMap<>();
        public final Map<String, ScannedPath> scannedPathMapping = new HashMap<>();
        public final Map<String, String> tableStructMapping = new HashMap<>();
        public long lastTimeChecked = 0L;
    }
}