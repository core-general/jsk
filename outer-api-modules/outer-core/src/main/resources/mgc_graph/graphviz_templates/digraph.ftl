digraph G {
rankdir=LR;
<#list model.edges as edge>
    <#include "*/edge.ftl">
</#list>
}