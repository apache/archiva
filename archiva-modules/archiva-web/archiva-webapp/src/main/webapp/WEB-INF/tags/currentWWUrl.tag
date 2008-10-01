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
<%@ attribute name="url" %>

<c:set var="currentUrl">
  <s:url/>
</c:set>
<c:if test="${!empty (action) && !empty (namespace)}">
  <c:set var="url">
    <s:url action="%{action}" namespace="%{namespace}"/>
  </c:set>
</c:if>
<c:set var="text">
  <jsp:doBody/>
</c:set>
<c:choose>
  <c:when test="${currentUrl == url}">
    <b>

    ${text}

    </b>
  </c:when>
  <c:otherwise>
    <a href="${url}">

    ${text}

    </a>
  </c:otherwise>
</c:choose>
