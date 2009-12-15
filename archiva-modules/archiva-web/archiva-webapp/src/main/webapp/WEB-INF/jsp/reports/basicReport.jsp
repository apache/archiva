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
  <title>Reports</title>
  <s:head/>
</head>

<body>
<h1>Reports</h1>

<div id="contentArea">

  <c:forEach var="repository" items="${repositoriesMap}">
	<strong>Repository: ${repository.key}</strong>
	<c:forEach var="report" items='${repository.value}'>
	
	    <p>
      	<archiva:groupIdLink var="${report.namespace}" includeTop="true"/>
      	<c:set var="url">
        <s:url action="browseArtifact" namespace="/">
          <s:param name="groupId" value="%{#attr.report.namespace}"/>
          <s:param name="artifactId" value="%{#attr.report.project}"/>
        </s:url>
      	</c:set>
      	<a href="${url}">${report.project}</a> /
      	<strong>${report.version}</strong>
    	</p>
    
		<blockquote>${report.message}</blockquote>
	</c:forEach>
  </c:forEach>

  <c:set var="prevPageUrl">
    <s:url action="generateReport" namespace="/">
      <s:param name="groupId" />
      <s:param name="repositoryId" />
      <s:param name="rowCount" />
      <s:param name="page" value="%{#attr.page - 1}"/>
    </s:url>
  </c:set>
  <c:set var="nextPageUrl">
    <s:url action="generateReport" namespace="/">
      <s:param name="groupId" />
      <s:param name="repositoryId" />
      <s:param name="rowCount" />
      <s:param name="page" value="%{#attr.page + 1}"/>
    </s:url>
  </c:set>
  <s:set name="page" value="page"/>
  <c:if test="${page > 1}"><a href="${prevPageUrl}">&lt;&lt;</a></c:if>
  Page: ${page}
  <s:set name="lastPage" value="lastPage"/>
  <c:if test="${!lastPage}"><a href="${nextPageUrl}">&gt;&gt;</a></c:if>

</div>
</body>
</html>
