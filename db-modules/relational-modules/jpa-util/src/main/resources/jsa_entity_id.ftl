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
<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaPrimaryKeyOutput" -->

package ${model.packageName};
import lombok.NoArgsConstructor;
import sk.utils.ids.*;
import ${model.key.idType};
@NoArgsConstructor(onConstructor_ = {@Deprecated})
public class ${model.key.mainType} extends <#if model.key.idType == "java.util.UUID">IdUuid<#elseif model.key.idType == "java.lang.String">IdString<#elseif model.key.idType == "java.lang.Long">IdLong<#elseif model.key.idType == "java.lang.Integer">IdLong<#else>IdBase<${model.key.idType}></#if> {

public ${model.key.mainType}(${model.key.idType} id) {
super(id);
}
}
