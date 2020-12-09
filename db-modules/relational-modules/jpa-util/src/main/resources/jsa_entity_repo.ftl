<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaEntityOutput" -->

package ${model.packageName};

import sk.db.relational.utils.ReadWriteRepo;

public interface ${model.model.cls}Repo extends ReadWriteRepo<${model.model.cls}, ${model.model.getIdField().mainType}> {}