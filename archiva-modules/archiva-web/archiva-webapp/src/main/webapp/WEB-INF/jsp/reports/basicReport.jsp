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

  <s:set name="reports" value="reports"/>  
    
  <c:forEach var="repository" items="${repositoriesMap}">
	<strong>Repository: ${repository.key}</strong>
	<c:forEach var="report" items='${repository.value}'>
	
	    <p>
      	<archiva:groupIdLink var="${report.groupId}" includeTop="true"/>
      	<c:set var="url">
        <s:url action="browseArtifact" namespace="/">
          <s:param name="groupId" value="%{#attr.report.groupId}"/>
          <s:param name="artifactId" value="%{#attr.report.artifactId}"/>
        </s:url>
      	</c:set>
      	<a href="${url}">${report.artifactId}</a> /
      	<strong>${report.version}</strong>
    	</p>
    
		<blockquote>${report.message}</blockquote>
	</c:forEach>
  </c:forEach>

  <s:set name="page" value="page"/>
  <c:if test="${page > 1}"><a href="<s:property value='prev' />">&lt;&lt;</a></c:if>
  Page: ${page}
  <s:set name="isLastPage" value="isLastPage"/>
  <c:if test="${!isLastPage}"><a href="<s:property value='next' />">&gt;&gt;</a></c:if>

</div>
</body>
</html>
