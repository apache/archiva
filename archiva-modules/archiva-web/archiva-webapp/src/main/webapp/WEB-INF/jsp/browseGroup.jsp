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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="archiva"   uri="/WEB-INF/taglib.tld" %>

<html>
<head>
  <title>Browse Repository</title>
  <s:head/>
</head>

<body>

<h1>Browse Repository</h1>

<div id="contentArea">
  <p>
    <archiva:groupIdLink var="${results.selectedGroupId}" includeTop="true" />
  </p>

  <div id="nameColumn">
    <h2>Groups</h2>
    <ul>
      <c:forEach items="${results.groupIds}" var="groupId">
        <c:set var="url">
          <s:url action="browseGroup" namespace="/">
            <s:param name="groupId" value="%{#attr.groupId}"/>
          </s:url>
        </c:set>
        <li><a href="${url}">${groupId}/</a></li>
      </c:forEach>
    </ul>
  </div>
  
  <c:if test="${not empty results.versions}">
    <div id="nameColumn">
      <h2>Versions</h2>
      <ul>
        <c:forEach items="${results.versions}" var="version">
          <c:set var="url">
            <s:url action="browseVersion" namespace="/">
              <s:param name="groupId" value="%{#attr.results.selectedGroupId}"/>
              <s:param name="version" value="%{#attr.version}"/>
            </s:url>
          </c:set>
          <li><a href="${url}">${version}/</a></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>  

  <c:if test="${not empty results.artifacts}">
    <div id="nameColumn">
      <h2>Artifacts</h2>
      <ul>
        <c:forEach items="${results.artifacts}" var="artifactId">
          <c:set var="url">
            <s:url action="browseArtifact" namespace="/">
              <s:param name="groupId" value="%{#attr.groupId}"/>
              <s:param name="artifactId" value="%{#attr.artifactId}"/>
            </s:url>
          </c:set>
          <li><a href="${url}">${artifactId}/</a></li>
        </c:forEach>
      </ul>
    </div>
  </c:if>  
    

</div>

</body>
</html>
