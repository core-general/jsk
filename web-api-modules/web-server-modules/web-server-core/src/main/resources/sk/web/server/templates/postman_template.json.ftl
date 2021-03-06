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
<#-- @ftlvariable name="server_name" type="java.lang.String" -->
<#-- @ftlvariable name="postman_id" type="java.lang.String" -->
<#-- @ftlvariable name="url_var_id" type="java.lang.String" -->
<#-- @ftlvariable name="methods" type="java.util.Set<sk.web.infogatherer.WebMethodInfo>" -->
<#-- @ftlvariable name="additionalParams" type="java.util.Map<String,String>" -->
{
"info": {
"name": "${server_name}",
"_postman_id": "${postman_id}",
"description": "",
"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
},
"item":[
<#list methods as method>
    {
    "name": "Method: ${method.fullMethodPath}",
    "description": "",
    "request": {
    "method": "${method.getType().simpleMethod}",
    "header": [
    {
    "key": "Accept-Encoding",
    "value": "gzip"
    }
    ],
    "body": {
    "mode": "formdata",
    "formdata": [

    <#list method.paramAndTypes as param>
        {
        "key": "${param.name}",
        "value": "Type: ${param.getTypeName()}",
        "type": "text",
        "description": "Main parameter"
        },
    </#list>
    <#list additionalParams as key,val>
        {
        "key": "${key}",
        "value": "${val}",
        "type": "text",
        "description": "Additional parameter"
        },
    </#list>
    {
    "key": "no_meaning_mark",
    "value": "no_meaning_mark",
    "type": "text",
    "description": "No meaning mark"
    }
    ]
    },
    "url": {
    "raw": "{{url}}${method.fullMethodPath}",
    "host": ["{{url}}"],
    "path": [${method.postmanize()}],
    "query": []
    },
    "description": ""
    }
    }<#if method_has_next>,</#if>
</#list>
],
"variable": [
{
"id": "${url_var_id}",
"key": "url",
"value": "http://localhost:8080/",
"type": "text"
}
]
}
