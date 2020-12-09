package sk.db.util.generator.model.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import sk.db.util.generator.model.sql.metainfo.JsaMetaInfo;
import sk.db.util.generator.model.sql.metainfo.JsaMetaType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class JsaTableInfo {
    String tableName;
    List<JsaTableColumn> fields;

    public JsaTableInfo(CreateTable table,
            Map<X2<String, String>, Map<JsaMetaType, JsaMetaInfo>> tableFieldsToMetaInfos) {
        this.tableName = table.getTable().getName();
        fields = table.getColumnDefinitions().stream()
                .map($ -> {
                    final String fieldName = $.getColumnName().toLowerCase();
                    return new JsaTableColumn(fieldName,
                            JsaDbColumnType.parse($.getColDataType().getDataType().toUpperCase()),
                            $.getColumnSpecs() != null
                                    && $.getColumnSpecs().containsAll(Cc.l("PRIMARY", "KEY")),
                            !($.getColumnSpecs() != null
                                    && $.getColumnSpecs().containsAll(Cc.l("NOT", "NULL"))),
                            O.ofNull(tableFieldsToMetaInfos.get(X.x(tableName, fieldName)))
                    );
                }).collect(Cc.toL());
    }
}
