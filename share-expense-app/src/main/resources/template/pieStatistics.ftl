{series : [{name: '${title}',type: 'pie',data: [<#if dataList??><#list dataList as data>{value:${data.value?c}, name:'${data.label}'}<#sep>,</#sep></#list></#if>]}]}