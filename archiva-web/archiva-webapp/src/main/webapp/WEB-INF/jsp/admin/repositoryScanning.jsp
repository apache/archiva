<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem"%>
<%@ taglib prefix="archiva" uri="http://maven.apache.org/archiva"%>

<html>
<head>
<title>Administration - Repository Scanning</title>
<ww:head />
</head>

<body>

<h1>Administration - Repository Scanning</h1>

<div id="contentArea">

<ww:actionerror /> 
<ww:actionmessage /> 

<c:url var="iconDeleteUrl" value="/images/icons/delete.gif" /> 
<c:url var="iconCreateUrl" value="/images/icons/create.png" /> 
<ww:url id="removeFiletypePatternUrl" action="repositoryScanning" method="removeFiletypePattern" /> 
<ww:url id="addFiletypePatternUrl"    action="repositoryScanning" method="addFiletypePattern" /> 
   
<script type="text/javascript">
<!--
  function removeFiletypePattern(filetypeId, pattern)
  {
     var f = document.getElementById('filetypeForm');
     
     f.action = "${removeFiletypePatternUrl}";
     f['pattern'].value = pattern;
     f['fileTypeId'].value = filetypeId;
     f.submit();
  }
  
  function addFiletypePattern(filetypeId, newPatternId)
  {
     var f = document.getElementById('filetypeForm');
     
     f.action = "${addFiletypePatternUrl}";
     f['pattern'].value = document.getElementById(newPatternId).value;
     f['fileTypeId'].value = filetypeId;
     f.submit();
  }
//-->
</script>

<div class="admin">
<h2>Repository Scanning - File Types</h2>

<c:choose>
  <c:when test="${empty(fileTypeMap)}">
    <%-- No File Types. Eeek! --%>
    <strong>There are no file types configured.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the filetypes. --%>

    <ww:form method="post" action="repositoryScanning" 
             namespace="/admin" validate="false" 
             id="filetypeForm" theme="simple">
      <input type="hidden" name="pattern" />
      <input type="hidden" name="fileTypeId" />
    </ww:form>

    <ww:url id="addFiletypePatternUrl" action="repositoryScanning" method="addFiletypePattern" />

    <c:forEach items="${fileTypeIds}" var="filetypeId">

      <div class="filetype">

      <div class="controls"><%-- Does this even make sense for file types? --%></div>

      <h3 class="filetype">${filetypeId}</h3>

      <table>
        <c:forEach items="${fileTypeMap[filetypeId].patterns}" var="pattern" varStatus="i">
          <c:choose>
            <c:when test='${(i.index)%2 eq 0}'>
              <c:set var="bgcolor" value="even" scope="page" />
            </c:when>
            <c:otherwise>
              <c:set var="bgcolor" value="odd" scope="page" />
            </c:otherwise>
          </c:choose>
          
          <c:set var="escapedPattern" value="${fn:escapeXml(pattern)}" scope="page" />
          
          <tr>
            <td class="pattern ${bgcolor}">
              <code>${escapedPattern}</code>
            </td>
            <td class="controls ${bgcolor}">
              <ww:a href="#" title="Remove [${escapedPattern}] Pattern from [${filetypeId}]"
                    onclick="removeFiletypePattern( '${filetypeId}', '${escapedPattern}' )" 
                    theme="simple">
                <img src="${iconDeleteUrl}" />
              </ww:a>
            </td>
          </tr>
        </c:forEach>
        <tr>
          <td>
            <ww:textfield size="40" 
                          id="newpattern_${i.index}"
                          theme="simple" />
          </td>
          <td>
            <ww:a href="#" 
                  title="Add Pattern to [${filetypeId}]"
                  onclick="addFiletypePattern( '${filetypeId}', 'newpattern_${i.index}' )"
                  theme="simple">
              <img src="${iconCreateUrl}" />
            </ww:a>
          </td>
        </tr>
      </table>

      </div>
    </c:forEach>

  </c:otherwise>
</c:choose>

<h2>Repository Scanning - Consumers of Good Content</h2>

<c:choose>
  <c:when test="${empty(goodConsumers)}">
    <%-- No Good Consumers. Eeek! --%>
    <strong>There are no good consumers configured.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the consumers. --%>

    <table>
      <c:forEach items="${goodConsumers}" var="consumer" varStatus="i">
        <c:choose>
          <c:when test='${(i.index)%2 eq 0}'>
            <c:set var="rowColor" value="dark" scope="page" />
          </c:when>
          <c:otherwise>
            <c:set var="rowColor" value="lite" scope="page" />
          </c:otherwise>
        </c:choose>

        <tr>
          <td><code>${consumer}</code></td>
          <td><img src="<c:url value="/images/icons/delete.gif" />" /></td>
        </tr>
      </c:forEach>
    </table>

  </c:otherwise>
</c:choose>


<h2>Repository Scanning - Consumers of Bad Content</h2>

<c:choose>
  <c:when test="${empty(badConsumers)}">
    <%-- No Bad Consumers. Eeek! --%>
    <strong>There are no bad consumers configured.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the consumers. --%>

    <table>
      <c:forEach items="${badConsumers}" var="consumer" varStatus="i">
        <c:choose>
          <c:when test='${(i.index)%2 eq 0}'>
            <c:set var="rowColor" value="dark" scope="page" />
          </c:when>
          <c:otherwise>
            <c:set var="rowColor" value="lite" scope="page" />
          </c:otherwise>
        </c:choose>

        <tr>
          <td><code>${consumer}</code></td>
          <td><img src="<c:url value="/images/icons/delete.gif" />" /></td>
        </tr>
      </c:forEach>
    </table>

  </c:otherwise>
</c:choose></div>
</body>
</html>
