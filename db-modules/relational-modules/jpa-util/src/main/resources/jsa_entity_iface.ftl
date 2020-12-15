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
