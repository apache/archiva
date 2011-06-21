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

<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="action" %>
<%@ attribute name="namespace" %>
<%@ attribute name="id" %>
<%@ attribute name="url" %>
<%@ attribute name="useParams" required="false" %>

<c:set var="currentUrl">
  <s:url>
    <c:if test="${useParams}">
      <s:param name="groupId" value="%{groupId}"/>
      <s:param name="artifactId" value="%{artifactId}"/>
      <s:param name="version" value="%{version}"/>
    </c:if>
  </s:url>
</c:set>
<c:if test="${!empty (action) && !empty (namespace)}">
  <c:set var="url">
    <s:url action="%{#attr.action}" namespace="%{#attr.namespace}" id="%{#attr.id}"/>
  </c:set>
</c:if>
<c:set var="text">
  <jsp:doBody/>
</c:set>
<!--URL: <c:out value="${url}"/>
Current URL: <c:out value="${currentUrl}"/> -->
<c:choose>
  <c:when test="${currentUrl == url}">
    <b>
    ${text}
    </b>
  </c:when>
  <c:otherwise>
    <a href="${url}" id="${id}">
    ${text}
    </a>
  </c:otherwise>
</c:choose>
