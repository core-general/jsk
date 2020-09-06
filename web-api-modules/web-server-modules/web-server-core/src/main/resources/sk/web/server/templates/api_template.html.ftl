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
<#-- @ftlvariable name="prefixPath" type="java.lang.String" -->
<#-- @ftlvariable name="mainComment" type="java.lang.String" -->
<#-- @ftlvariable name="methodList" type="java.util.Collection<sk.web.infogatherer.WebMethodInfo>" -->
<#-- @ftlvariable name="classList" type="java.util.Collection<sk.mvn.model.ApiDtoClassModel>" -->
<html lang="en">
<head>
    <style>
        body {
            background-color: black;
            color: rgb(162, 162, 162);
        }

        hr {
            height: 1px;
            background-color: rgb(144, 144, 144);
            border: none;
        }

        .comment {
            color: rgb(98, 151, 83);
        }

        .pMethodName {
            margin-top: 15.0px;
            margin-bottom: 5.0px;
        }

        .methodName {
            font-weight: bold;
            font-size: larger;
        }

        .pParameter {
            margin-left: 60.0px;
            margin-top: 5.0px;
            margin-bottom: 5.0px;
        }

        .keySymb {
            color: rgb(204, 118, 49);
        }

        .keyWord {
            color: rgb(204, 118, 49);
        }

        .parameter {

        }

        .type {
            color: rgb(37, 124, 163);
        }

        .pException {
            margin-left: 60.0px;
            margin-top: 5.0px;
            margin-bottom: 5.0px;
        }

        .exceptionName {
            color: rgb(200, 95, 96);
        }

        .pClassField {
            margin-left: 30.0px;
            margin-top: 5.0px;
            margin-bottom: 5.0px;
        }

        .classField {

        }
    </style>
    <title>API for ${prefixPath}</title>
</head>
<body>
<hr/>
<#if mainComment??>
    <h2 class="comment">
        /*
        General Description for ${prefixPath}:<br>
        ${mainComment}
        */
    </h2>
    <hr/>
</#if>
<h2 class="comment">
    /* Methods: */
</h2>
<#list methodList as method>

    <p class="pMethodName">
        <#if method.precompiledModel.commentOrNull??>
            <span class="comment">/* ${method.precompiledModel.commentOrNull} */</span><br>
        </#if>
        <span class="type">${method.precompiledModel.returnInfoOrVoid.typeOrNull} </span>
        <#if method.precompiledModel.returnInfoOrVoid.commentOrNull??>
            <span class="comment">/* Return value comment: ${method.precompiledModel.returnInfoOrVoid.commentOrNull} */</span><br>
        </#if>
        <span class="methodName">${method.fullMethodPath}</span> <span class="keySymb">(</span>
        <#if method.precompiledModel.params?size == 0><span class="keySymb">)</span></#if>
    </p>
    <#list method.precompiledModel.params as param>
        <p class="pParameter">
            <span class="type">${param.typeOrNull}</span><span class="parameter"> ${param.name} </span>
            <#if param?has_next><span class="keySymb">,</span></#if>
            <#if param.commentOrNull??>
                <span class="comment">/* ${param.commentOrNull} */</span>
            </#if>
            <#if !(param?has_next)><span class="keySymb">)</span></#if>
        </p>
    </#list>

    <#if method.precompiledModel.exceptions?size gt 0>
        <p class="pException">
            <span class="keyWord">throws</span> <span class="comment">/* exception codes: */</span>
        </p>
        <#list method.precompiledModel.exceptions as exception>
            <p class="pException">
                <span class="exceptionName">${exception.exceptionName}</span>
                <#if exception?has_next><span class="keySymb">,</span></#if>
                <#if exception.comment??>
                    <span class="comment">/* ${exception.comment} */</span>
                </#if>
            </p>
        </#list>
    </#if>
</#list>
<hr/>
<h2 class="comment">
    /* Class model: */
</h2>
<#list classList as classModel>
    <p class="pMethodName">
        <#if classModel.commentOrNull??>
            <span class="comment">/* ${classModel.commentOrNull} */</span><br>
        </#if>
        <span class="keyWord"><#if classModel.isEnum()>enum <#else>class </#if></span>
        <span class="methodName type">${classModel.name}</span>
        <#if classModel.parentTypeOrNull??>
            <span class="keyWord">extends</span>
            <span class="methodName type">${classModel.parentTypeOrNull}</span>
        </#if>
        <span class="keySymb">{</span>
    </p>
    <#list classModel.fields as field>
        <p class="pClassField">
            <#if field.typeOrNull??><span class="type">${field.typeOrNull}</span></#if>
            <span class="classField">${field.name}</span>
            <span class="keySymb"><#if field.typeOrNull??>;<#else><#if field?has_next>,</#if></#if></span>
            <#if field.commentOrNull??><span class="comment">/* ${field.commentOrNull} */</span></#if>
        </p>
    </#list>
    <span class="keySymb">}</span>
</#list>
</body>
</html>
