<#--
 #%L
 Swiss Knife
 %%
 Copyright (C) 2019 - 2023 Core General
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
<#-- @ftlvariable name="offset" type="java.lang.String" -->
<#-- @ftlvariable name="limit" type="java.lang.String" -->
<#-- @ftlvariable name="orderByDirection" type="java.lang.String" -->
<#-- @ftlvariable name="orderByField" type="java.lang.String" -->
<#-- @ftlvariable name="itemSelector" type="java.lang.String" -->
<#-- @ftlvariable name="defaultWhere" type="java.lang.String" -->
<#-- @ftlvariable name="table" type="java.lang.String" -->

select *
from ${table}
where ${defaultWhere} ${itemSelector}
order by ${orderByField} ${orderByDirection}
limit ${limit} offset ${offset}
