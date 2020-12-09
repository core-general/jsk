<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaEntityOutput" -->

package ${model.packageName};

import java.time.ZonedDateTime;
import java.util.UUID;

public interface ${model.model.interfce} {
<#list model.model.fields as field>
    <#switch field.category>
        <#case "ID">
            ${field.mainType} get${field.capName()}();
            void set${field.capName()}(${field.mainType} ${field.fieldName});
            <#break>
        <#case "RELATION_IN">
        <#case "RELATION_OUT">
            ${field.mainType} get${field.capName()}();

            ${field.relatedType?replace("Jpa", "")} get${field.capName()?replace("Id", "")}();

            void set${field.capName()}(${field.mainType} ${field.fieldName});
            <#break>
        <#case "ENUM">
            ${field.mainType} get${field.capName()}();

            void set${field.capName()}(${field.mainType} ${field.fieldName});
            <#break>
        <#case "ZDT">
            ${field.mainType} get${field.capName()}();
            void set${field.capName()}(${field.mainType} ${field.fieldName});
            <#break>
        <#case "JSONB">
            ${field.mainType} get${field.capName()}();

            void set${field.capName()}(${field.mainType} ${field.fieldName});
            <#break>
        <#case "OTHER">
            ${field.mainType} get${field.capName()}();

            void set${field.capName()}(${field.mainType} ${field.fieldName});
            <#break>
    </#switch>
</#list>
}