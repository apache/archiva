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
<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva"   uri="/WEB-INF/taglib.tld" %>

<html>
<head>
<title>Administration - Network Proxies</title>
<s:head />
</head>

<body>

<h1>Administration - Network Proxies</h1>

<div id="contentArea">

  <%-- changed the structure of displaying errorMessages & actionMessages in order for them to be escaped. --%>
  <s:if test="hasActionErrors()">
      <ul>
      <s:iterator value="actionErrors">
          <li><span class="errorMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
  </s:if>
  <s:if test="hasActionMessages()">
      <ul>
      <s:iterator value="actionMessages">
          <li><span class="actionMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
  </s:if>

<div class="admin">
<div class="controls">
<redback:ifAuthorized
  permission="archiva-manage-configuration">
  <s:url id="addNetworkProxyUrl" action="addNetworkProxy" />
  <s:a href="%{addNetworkProxyUrl}">
    <img src="<c:url value="/images/icons/create.png" />" />
        Add Network Proxy</s:a>
</redback:ifAuthorized></div>
<h2>Network Proxies</h2>

<c:choose>
  <c:when test="${empty (networkProxies)}">
    <%-- No Local Repositories. --%>
    <strong>There are no network proxies configured yet.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the repositories. --%>

    <c:forEach items="${networkProxies}" var="proxy" varStatus="i">
      <c:choose>
        <c:when test='${(i.index)%2 eq 0}'>
          <c:set var="rowColor" value="dark" scope="page" />
        </c:when>
        <c:otherwise>
          <c:set var="rowColor" value="lite" scope="page" />
        </c:otherwise>
      </c:choose>

      <div class="netproxy ${rowColor}">

      <div class="controls">
      <redback:ifAnyAuthorized
        permissions="archiva-manage-configuration">
        <s:token/>
        <s:url id="editNetworkProxyUrl" encode="true" action="editNetworkProxy">
          <s:param name="proxyid" value="%{#attr.proxy.id}" />
        </s:url>
        <s:url id="deleteNetworkProxyUrl" encode="true" action="deleteNetworkProxy" method="confirm">
          <s:param name="proxyid" value="%{#attr.proxy.id}" />
          <s:param name="struts.token.name">struts.token</s:param>
          <s:param name="struts.token"><s:property value="struts.token"/></s:param>
        </s:url>
        <s:a href="%{editNetworkProxyUrl}">
          <img src="<c:url value="/images/icons/edit.png" />" />
            Edit Network Proxy</s:a>
        <s:a href="%{deleteNetworkProxyUrl}">
          <img src="<c:url value="/images/icons/delete.gif" />" />
            Delete Network Proxy</s:a>
      </redback:ifAnyAuthorized></div>

      <%-- used c:out in displaying EL's for them to be escaped.  --%>
      <table class="infoTable">
        <tr>
          <th>Identifier</th>
          <td><code><c:out value="${proxy.id}" /></code></td>
        </tr>
        <tr>
          <th>Protocol</th>
          <td><c:out value="${proxy.protocol}" /></td>
        </tr>
        <tr>
          <th>Host</th>
          <td><c:out value="${proxy.host}" /></td>
        </tr>
        <tr>
          <th>Port</th>
          <td><c:out value="${proxy.port}" /></td>
        </tr>
        <c:if test="${not empty (proxy.username)}">
          <tr>
            <th>Username</th>
            <td><c:out value="${proxy.username}" /></td>
          </tr>
          <c:if test="${not empty (proxy.password)}">
            <tr>
              <th>Password</th>
              <td>&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;</td>
            </tr>
          </c:if>
        </c:if>
      </table>

      </div>
    </c:forEach>

  </c:otherwise>
</c:choose>
</div>

</div>

</body>
</html>
