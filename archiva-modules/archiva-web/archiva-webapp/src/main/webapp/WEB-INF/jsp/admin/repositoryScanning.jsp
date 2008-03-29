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
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
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
     var f = document.forms['filetypeForm'];
          
     f.action = "${addFiletypePatternUrl}";     
     f.elements['pattern'].value = document.getElementById(newPatternId).value;
     f.elements['fileTypeId'].value = filetypeId;
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

    <c:forEach items="${fileTypeIds}" var="filetypeId" varStatus="j">

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
                          id="newpattern_${j.index}"
                          theme="simple" />
          </td>
          <td>
            <ww:a href="#" 
                  title="Add Pattern to [${filetypeId}]"
                  onclick="addFiletypePattern( '${filetypeId}', 'newpattern_${j.index}' )"
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

<h2>Repository Scanning - Consumers of Known Content</h2>

<c:choose>
  <c:when test="${empty(knownContentConsumers)}">
    <%-- No Good Consumers. Eeek! --%>
    <strong>There are no consumers of known content available.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the consumers. --%>

    <ww:form method="post" action="repositoryScanning!updateKnownConsumers" 
             namespace="/admin" validate="false" theme="simple">
    <table class="consumers">
      <tr>
        <th>&nbsp;</th>
        <th>Enabled?</th>
        <th>ID</th>
        <th>Description</th>
      </tr>
      <c:forEach items="${knownContentConsumers}" var="consumer" varStatus="i">
        <c:choose>
          <c:when test='${(i.index)%2 eq 0}'>
            <c:set var="bgcolor" value="even" scope="page" />
          </c:when>
          <c:otherwise>
            <c:set var="bgcolor" value="odd" scope="page" />
          </c:otherwise>
        </c:choose>

        <tr>
          <td class="${bgcolor}">
            <input type="checkbox" name="enabledKnownContentConsumers" theme="simple" value="${consumer.id}" <c:if test="${consumer.enabled}">checked</c:if> />
          </td>
          <td class="${bgcolor}">
            <c:if test="${consumer.enabled}">
              <strong>enabled</strong>
            </c:if>
          </td>
          <td class="${bgcolor}">
            <code>${consumer.id}</code>
          </td>
          <td class="${bgcolor}">${consumer.description}</td>
        </tr>
      </c:forEach>
      <tr>
        <td colspan="4">
          <ww:submit value="Update Consumers" />
        </td>
      </tr>
    </table>
    </ww:form>

  </c:otherwise>
</c:choose>


<h2>Repository Scanning - Consumers of Invalid Content</h2>

<c:choose>
  <c:when test="${empty(invalidContentConsumers)}">
    <%-- No Consumers. Eeek! --%>
    <strong>There are no consumers of invalid content available.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the consumers. --%>

    <ww:form method="post" action="repositoryScanning!updateInvalidConsumers" 
             namespace="/admin" validate="false" theme="simple">
    <table class="consumers">
      <tr>
        <th>&nbsp;</th>
        <th>Enabled?</th>
        <th>ID</th>
        <th>Description</th>
      </tr>
      <c:forEach items="${invalidContentConsumers}" var="consumer" varStatus="i">
        <c:choose>
          <c:when test='${(i.index)%2 eq 0}'>
            <c:set var="bgcolor" value="even" scope="page" />
          </c:when>
          <c:otherwise>
            <c:set var="bgcolor" value="odd" scope="page" />
          </c:otherwise>
        </c:choose>

        <tr>
          <td class="${bgcolor}">
            <input type="checkbox" name="enabledInvalidContentConsumers" theme="simple" value="${consumer.id}" <c:if test="${consumer.enabled}">checked</c:if> />
          </td>
          <td class="${bgcolor}">
            <c:if test="${consumer.enabled}">
              <strong>enabled</strong>
            </c:if>
          </td>
          <td class="${bgcolor}">
            <code>${consumer.id}</code>
          </td>
          <td class="${bgcolor}">${consumer.description}</td>
        </tr>
      </c:forEach>
      <tr>
        <td colspan="4">
          <ww:submit value="Update Consumers" />
        </td>
      </tr>
    </table>
    </ww:form>

  </c:otherwise>
</c:choose></div>
</body>
</html>
