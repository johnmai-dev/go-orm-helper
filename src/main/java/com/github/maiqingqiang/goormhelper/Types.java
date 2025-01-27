package com.github.maiqingqiang.goormhelper;

import com.alibaba.druid.DbType;
import com.github.maiqingqiang.goormhelper.sql2struct.ISQL2Struct;
import com.github.maiqingqiang.goormhelper.sql2struct.impl.SQL2GoFrameStruct;
import com.github.maiqingqiang.goormhelper.sql2struct.impl.SQL2GormStruct;
import com.github.maiqingqiang.goormhelper.sql2struct.impl.SQL2Struct;
import com.github.maiqingqiang.goormhelper.sql2struct.impl.SQL2XormStruct;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.regex.Pattern;

public interface Types {

    List<String> OPERATOR_EXPR = List.of(
            "%s = ?",
            "%s <> ?",
            "%s != ?",
            "%s IN ?",
            "%s NOT IN ?",
            "%s LIKE ?",
            "%s > ?",
            "%s >= ?",
            "%s < ?",
            "%s <= ?",
            "%s <=> ?",
            "%s IS NULL",
            "%s IS NOT NULL",
            "%s BETWEEN ? AND ?",
            "%s NOT BETWEEN ? AND ?"
    );

    List<String> LOGICAL_OPERATOR_EXPR = List.of("AND", "OR", "XOR", "NOT");
    List<String> USE_LOGICAL_OPERATOR_SCENE = List.of(" ?", " IS NULL", " IS NOT NULL");

    String MODEL_ANNOTATION = "@Model";
    Pattern MODEL_ANNOTATION_PATTERN = Pattern.compile(MODEL_ANNOTATION + "\\((.*?)\\)");
    String TABLE_ANNOTATION = "@Table";
    Pattern TABLE_ANNOTATION_PATTERN = Pattern.compile(TABLE_ANNOTATION + "\\((.*?)\\)");

    String TABLE_NAME_FUNC = "TableName";
    List<String> EXCLUDED_SCAN_LIST = List.of(
            "vendor", "node_modules", "third_party", "third-party", "third party", "test", "tests", "example", "examples"
    );

    enum ORM {
        AskEveryTime(GoORMHelperBundle.message("orm.AskEveryTime")),
        General(GoORMHelperBundle.message("orm.General")),
        Gorm("Gorm"),
        Xorm("Xorm"),
        GoFrame("GoFrame");

        private final String name;

        ORM(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public ISQL2Struct sql2Struct(Project project, String sql, DbType dbType) {
            return switch (this) {
                case General -> new SQL2Struct(project, sql, dbType);
                case Gorm -> new SQL2GormStruct(project, sql, dbType);
                case Xorm -> new SQL2XormStruct(project, sql, dbType);
                case GoFrame -> new SQL2GoFrameStruct(project, sql, dbType);
                default -> null;
            };
        }
    }

    enum Database {
        AskEveryTime(GoORMHelperBundle.message("database.AskEveryTime")),
        MySQL("MySQL"),
        PostgreSQL("PostgreSQL");

        private final String name;

        Database(String name) {
            this.name = name;
        }

        public DbType toDbType() {
            return switch (this) {
                case MySQL -> DbType.mysql;
                case PostgreSQL -> DbType.postgresql;
                default -> null;
            };
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum TagMode {
        Compact(GoORMHelperBundle.message("tagMode.Compact")),
        Full(GoORMHelperBundle.message("tagMode.Full"));

        private final String name;

        @Override
        public String toString() {
            return name;
        }

        TagMode(String name) {
            this.name = name;
        }
    }
}
