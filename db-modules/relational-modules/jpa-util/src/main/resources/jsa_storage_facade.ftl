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
<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaStorageOutput" -->

package ${model.packageName};

import sk.utils.functional.*;
import sk.db.relational.spring.services.*;

public interface ${model.prefix}StorageFacade extends RdbTransactionManager {
<#list model.entites as entity>
    O<${entity.getIFace()}> get${entity.simple?cap_first}ById(${entity.getIdField().mainType} ${entity.simple}Id);

    ${entity.getIFace()} new${entity.getIFace()}(
    <#list entity.getFieldForFactory() as field>
        <#if field.nullable>O<${field.mainType}><#else>${field.mainType}</#if> ${field.fieldName}<#sep>,</#sep>
    </#list>
    );
</#list>
}
