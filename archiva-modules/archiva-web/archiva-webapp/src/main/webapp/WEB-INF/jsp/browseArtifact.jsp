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

<%@ page contentType="text/html; charset=UTF-8" %>
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
  <div id="nameColumn">
    <p>
      <archiva:groupIdLink var="${groupId}" includeTop="true" />
      <strong>${artifactId}</strong>
    </p>

    <h2>Versions</h2>
    <ul>
      <s:set name="versions" value="versions"/>
      <c:forEach items="${versions}" var="version">
        <c:set var="url">
          <s:url action="showArtifact" namespace="/">
            <s:param name="groupId" value="%{#attr.groupId}"/>
            <s:param name="artifactId" value="%{#attr.artifactId}"/>
            <s:param name="version" value="%{'#attr.version}"/>
          </s:url>
        </c:set>
        <li><a href="${url}">${version}/</a></li>
      </c:forEach>
    </ul>
  </div>
</div>

</body>
</html>
