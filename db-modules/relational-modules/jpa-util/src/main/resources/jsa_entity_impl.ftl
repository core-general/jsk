<#--
 #%L
 Swiss Knife
 %%
 Copyright (C) 2019 - 2020 Core General
 %%
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 #L%
-->
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
public class ${model.model.cls} <#if model.model.hasCreatedAt() || model.model.hasUpdatedAt()>extends JpaWithContextAndCreatedUpdated</#if> implements ${model.model.getIFace()} {
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
        <#case "PG_ENUM">
            @Column(name = "${field.columnName}")
            @Type(type = UtPgEnumToEnumUserType.type, parameters = {@Parameter(name = UtPgEnumToEnumUserType.param, value = ${field.mainType}.type)})
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
