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
    // ${entity.interfce}
    // ------------------------------------------------
    @Override
    public O<${entity.interfce}> get${entity.simple?cap_first}ById(${entity.getIdField().mainType} ${entity.simple}Id){
    return O.of(${entity.simple}Repo.findById(${entity.simple}Id).map($->$));
    }

    @Override
    public ${entity.interfce} new${entity.interfce}(
    <#list entity.getFieldForFactory() as field>
        <#if field.nullable>O<${field.mainType}><#else>${field.mainType}</#if> ${field.fieldName}<#sep>,</#sep>
    </#list>
    ){
    return new ${entity.cls}(<#list entity.fields as field><#if field.category == "VERSION" ||  field.fieldName == "createdAt" || field.fieldName == "updatedAt">null<#elseif field.category == "RELATION_IN" || field.category == "RELATION_OUT"><#if field.nullable>${field.fieldName}.orElse(null)<#else>${field.fieldName}</#if>, null<#else><#if field.nullable>${field.fieldName}.orElse(null)<#else>${field.fieldName}</#if></#if><#sep>, </#sep></#list>);
    }


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