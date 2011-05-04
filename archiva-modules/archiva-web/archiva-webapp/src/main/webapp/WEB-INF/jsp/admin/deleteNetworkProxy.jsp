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

<html>
<head>
  <title>Admin: Delete Network Proxy</title>
  <s:head/>
</head>

<body>

<h1>Admin: Delete Network Proxy</h1>

  <%-- changed the structure of displaying errorMessages in order for them to be escaped. --%>
  <s:if test="hasActionErrors()">
      <ul>
      <s:iterator value="actionErrors">
          <li><span class="errorMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
  </s:if>

<div id="contentArea">

  <h2>Delete Network Proxy</h2>

  <blockquote>
    <strong><span class="statusFailed">WARNING:</span> This operation can not be undone.</strong>
  </blockquote>
  <%-- used c:out in displaying EL's for them to be escaped.  --%>
  <p>
      Are you sure you want to delete network proxy <code><c:out value="${proxyid}" /></code> ?
  </p>

  <s:form method="post" action="deleteNetworkProxy!delete" namespace="/admin" validate="true">
    <s:hidden name="proxyid"/>
    <s:token/>
    <s:submit value="Delete"/>
  </s:form>
</div>

</body>
</html>