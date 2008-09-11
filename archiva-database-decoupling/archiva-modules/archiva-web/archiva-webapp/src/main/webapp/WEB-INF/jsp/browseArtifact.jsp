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
<%@ taglib prefix="archiva" uri="http://archiva.apache.org" %>

<html>
<head>
  <title>Browse Repository</title>
  <ww:head/>
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
      <ww:set name="versions" value="versions"/>
      <c:forEach items="${versions}" var="version">
        <c:set var="url">
          <ww:url action="showArtifact" namespace="/">
            <ww:param name="groupId" value="%{'${groupId}'}"/>
            <ww:param name="artifactId" value="%{'${artifactId}'}"/>
            <ww:param name="version" value="%{'${version}'}"/>
          </ww:url>
        </c:set>
        <li><a href="${url}">${version}/</a></li>
      </c:forEach>
    </ul>
  </div>
</div>

</body>
</html>
