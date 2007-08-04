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
<%@ taglib prefix="archiva" uri="http://maven.apache.org/archiva" %>

<html>
<head>
  <title>Reports</title>
  <ww:head/>
</head>

<body>
<h1>Reports</h1>

<div id="contentArea">

  <ww:set name="reports" value="reports"/>
  <c:forEach items="${reports}" var="report">

    <p>
      <archiva:groupIdLink var="${report.groupId}" includeTop="true"/>

      <c:set var="url">
        <ww:url action="browseArtifact" namespace="/">
          <ww:param name="groupId" value="%{'${report.groupId}'}"/>
          <ww:param name="artifactId" value="%{'${report.artifactId}'}"/>
        </ww:url>
      </c:set>
      <a href="${url}">${report.artifactId}</a> /
      <strong>${report.version}</strong>
    </p>

    <blockquote>${report.message}</blockquote>
  </c:forEach>

  <ww:set name="page" value="page"/>
  <c:if test="${page > 1}"><a href="<ww:property value='prev' />">&lt;&lt;</a></c:if>
  Page: ${page}
  <ww:set name="isLastPage" value="isLastPage"/>
  <c:if test="${!isLastPage}"><a href="<ww:property value='next' />">&gt;&gt;</a></c:if>

</div>
</body>
</html>
