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

import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import net.bull.javamelody.MonitoredWithSpring;

import sk.utils.functional.*;
import sk.db.relational.spring.services.impl.*;

@MonitoredWithSpring
@Log4j2
public class ${model.prefix}StorageFacadeImpl extends RdbTransactionManagerImpl implements ${model.prefix}StorageFacade {
<#list model.entites as entity>
    private @Inject ${entity.cls}Repo ${entity.simple}Repo;
    private Q${entity.cls} q${entity.simple} = Q${entity.cls}.${entity.cls?uncap_first};
</#list>

<#list model.entites as entity>
    // ------------------------------------------------
    // region ${entity.getIFace()}
    // ------------------------------------------------
    @Override
    public O<${entity.getIFace()}> get${entity.simple?cap_first}ById(${entity.getIdField().mainType} ${entity.simple}Id){
    return O.of(${entity.simple}Repo.findById(${entity.simple}Id).map($->$));
    }

    @Override
    public ${entity.getIFace()} new${entity.getIFace()}(
    <#list entity.getFieldForFactory() as field>
        <#if field.nullable>O<${field.mainType}><#else>${field.mainType}</#if> ${field.fieldName}<#sep>,</#sep>
    </#list>
    ){
    return new ${entity.cls}(<#list entity.fields as field><#if field.category == "VERSION" ||  field.fieldName == "createdAt" || field.fieldName == "updatedAt">null<#elseif field.category == "RELATION_IN" || field.category == "RELATION_OUT"><#if field.nullable>${field.fieldName}.orElse(null)<#else>${field.fieldName}</#if>, null<#else><#if field.nullable>${field.fieldName}.orElse(null)<#else>${field.fieldName}</#if></#if><#sep>, </#sep></#list>);
    }
    // endregion

</#list>

@Override
protected void saveSingleItem(Object toSave) {
<#list model.entites as entity>
    if (toSave instanceof ${entity.cls}) {
    ${entity.simple}Repo.save((${entity.cls}) toSave);
    return;
    }
</#list>

throw new RuntimeException("Unknown transactional type: " + toSave.getClass());
}

}
