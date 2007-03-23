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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<html>
<head>
  <title>Reports</title>
  <ww:head/>
</head>

<body>

<h1>Reports</h1>

<div id="contentArea">

<c:forEach items="${reports}" var="report">
  <h3>
      ${report.groupId} : ${report.artifactId} : ${report.version} : ${report.classifier} : ${report.type}
  </h3>
  <ul>
    <c:forEach items="${report.results}" var="result">
      <li>
        <b>${result.reason}</b>
      </li>
    </c:forEach>
  </ul>
</c:forEach>
<c:if test="${empty(reports)}">
  <strong>No reports for any artifact.</strong>
</c:if>

</div>

</body>
</html>
