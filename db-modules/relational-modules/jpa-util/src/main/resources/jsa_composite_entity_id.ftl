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
<#-- @ftlvariable name="model" type="sk.db.util.generator.model.output.JsaEmbeddedKeyOutput" -->
<#-- @formatter:off-->
package ${model.packageName};

import sk.db.relational.types.*;
import sk.db.relational.model.*;

import lombok.*;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import java.io.Serializable;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;
<#list model.keys as field>
    import ${field.idType};
</#list>

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ${model.embeddedClassName} implements Serializable {
    public final static String type = "${model.packageName}.${model.embeddedClassName}";

<#list model.keys as field>
    @Column(name = "${field.columnName}")
    @Type(type = ${field.converterType}.type, parameters = {@Parameter(name = ${field.converterType}.param, value = ${field.mainType}.type)})
    ${field.mainType} ${field.fieldName};
</#list>
}