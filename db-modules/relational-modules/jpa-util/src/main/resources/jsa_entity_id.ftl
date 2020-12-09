<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaPrimaryKeyOutput" -->

package ${model.packageName};

import sk.utils.ids.*;
import ${model.key.idType};

public class ${model.key.mainType} extends <#if model.key.idType == "java.util.UUID">IdUuid<#elseif model.key.idType == "java.lang.String">IdString<#elseif model.key.idType == "java.lang.Long">IdLong<#elseif model.key.idType == "java.lang.Integer">IdLong<#else>IdBase<${model.key.idType}></#if> {
public final static String type = "${model.packageName}.${model.key.mainType}";

public ${model.key.mainType}(${model.key.idType} id) {
super(id);
}
}