<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaEntityOutput" -->

package ${model.packageName};

import sk.db.relational.types.*;
import sk.db.relational.model.*;

import lombok.*;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "${model.model.table}", schema = "${model.dbSchema}")
public class ${model.model.cls} <#if model.model.hasCreatedAt() || model.model.hasUpdatedAt()>extends JpaWithContextAndCreatedUpdated</#if> implements ${model.model.interfce} {
<#list model.model.fields as field>
    <#switch field.category>
        <#case "ID">
            @Id
            @Column(name = "${field.columnName}")
            @Type(type = ${field.converterType}.type, parameters = {@Parameter(name = ${field.converterType}.param, value = ${field.mainType}.type)})
            ${field.mainType} ${field.fieldName};
            <#break>
        <#case "RELATION_OUT">
        <#case "RELATION_IN">
            @Column(name = "${field.columnName}")
            @Type(type = ${field.converterType}.type, parameters = {@Parameter(name = ${field.converterType}.param, value = ${field.mainType}.type)})
            ${field.mainType} ${field.fieldName};
            @ManyToOne(fetch = FetchType.LAZY)
            @JoinColumn(name = "${field.columnName}", insertable = false, updatable = false)
            ${field.relatedType} ${field.fieldName?replace("Id", "")};
            <#break>
        <#case "ENUM">
            @Column(name = "${field.columnName}")
            @Type(type = UTEnumToString.type, parameters = {@Parameter(name = UTEnumToString.param, value = ${field.mainType}.type)})
            ${field.mainType} ${field.fieldName};
            <#break>
        <#case "ZDT">
            @Column(name = "${field.columnName}")
            @Type(type = ${field.converterType}.type)
            ${field.mainType} ${field.fieldName};
            <#break>
        <#case "JSONB">
            @Column(name = "${field.columnName}")
            @Type(type = ${field.converterType}.type, parameters = {@Parameter(name = ${field.converterType}.param, value = ${field.mainType}.type)})
            ${field.mainType} ${field.fieldName};
            <#break>
        <#case "VERSION">
            @Version
            ${field.mainType} ${field.fieldName};
            <#break>
        <#case "OTHER">
            @Column(name = "${field.columnName}")
            ${field.mainType} ${field.fieldName};
            <#break>
    </#switch>

</#list>
}