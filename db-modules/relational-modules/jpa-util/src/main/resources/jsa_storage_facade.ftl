<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaStorageOutput" -->

package ${model.packageName};

import sk.utils.functional.*;
import sk.db.relational.spring.services.*;

public interface ${model.prefix}StorageFacade extends RdbTransactionManager {
<#list model.entites as entity>
    O<${entity.interfce}> get${entity.simple?cap_first}ById(${entity.getIdField().mainType} ${entity.simple}Id);

    ${entity.interfce} new${entity.interfce}(
    <#list entity.getFieldForFactory() as field>
        <#if field.nullable>O<${field.mainType}><#else>${field.mainType}</#if> ${field.fieldName}<#sep>,</#sep>
    </#list>
    );
</#list>
}