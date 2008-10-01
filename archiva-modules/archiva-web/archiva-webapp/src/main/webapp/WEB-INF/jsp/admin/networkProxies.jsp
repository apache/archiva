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

<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva" uri="http://archiva.apache.org"%>

<html>
<head>
<title>Administration - Network Proxies</title>
<s:head />
</head>

<body>

<h1>Administration - Network Proxies</h1>

<div id="contentArea">

<s:actionerror /> <s:actionmessage />

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
        <s:url id="editNetworkProxyUrl" action="editNetworkProxy">
          <s:param name="proxyid" value="%{'${proxy.id}'}" />
        </s:url>
        <s:url id="deleteNetworkProxyUrl" action="deleteNetworkProxy" method="confirm">
          <s:param name="proxyid" value="%{'${proxy.id}'}" />
        </s:url>
        <s:a href="%{editNetworkProxyUrl}">
          <img src="<c:url value="/images/icons/edit.png" />" />
            Edit Network Proxy</s:a>
        <s:a href="%{deleteNetworkProxyUrl}">
          <img src="<c:url value="/images/icons/delete.gif" />" />
            Delete Network Proxy</s:a>
      </redback:ifAnyAuthorized></div>

      <table class="infoTable">
        <tr>
          <th>Identifier</th>
          <td><code>${proxy.id}</code></td>
        </tr>
        <tr>
          <th>Protocol</th>
          <td>${proxy.protocol}</td>
        </tr>
        <tr>
          <th>Host</th>
          <td>${proxy.host}</td>
        </tr>
        <tr>
          <th>Port</th>
          <td>${proxy.port}</td>
        </tr>
        <c:if test="${not empty (proxy.username)}">
          <tr>
            <th>Username</th>
            <td>${proxy.username}</td>
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
