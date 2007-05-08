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

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem" %>
<%@ taglib prefix="archiva" uri="http://maven.apache.org/archiva" %>

<html>
<head>
  <title>Administration - Repository Scanning</title>
  <ww:head/>
</head>

<body>

<h1>Administration - Repository Scanning</h1>

<div id="contentArea">

<ww:actionerror />
<ww:actionmessage />

<div class="admin">
  <h2>Repository Scanning - File Types</h2>

<c:choose>
  <c:when test="${empty(fileTypeMap)}">
    <%-- No File Types. Eeek! --%>
    <strong>There are no file types configured.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the filetypes. --%>
    
  <c:forEach items="${fileTypeMap}" var="filetype" varStatus="i">
    <c:choose>
      <c:when test='${(i.index)%2 eq 0}'>
        <c:set var="rowColor" value="dark" scope="page" />
      </c:when>
      <c:otherwise>
        <c:set var="rowColor" value="lite" scope="page" />
      </c:otherwise>
    </c:choose>

    <div class="filetype ${rowColor}">

      <div class="controls">
        <%-- Does this even make sense for file types? --%>
      </div>
      
      <h3 class="filetype">${filetype.key}</h3>

      <table>
      <c:forEach items="${filetype.value.patterns}" var="pattern" varStatus="i">
        <tr>
          <td>
            <code>${pattern}</code>
          </td>
          <td>
            <img src="<c:url value="/images/icons/delete.gif" />" />
          </td>
        </tr>
      </c:forEach>
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
        <td>
          <img src="<c:url value="/images/icons/delete.gif" />" />
        </td>
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
        <td>
          <img src="<c:url value="/images/icons/delete.gif" />" />
        </td>
      </tr>
    </c:forEach>
  </table>
  
  </c:otherwise>
</c:choose>
  

</div>

</body>
</html>
