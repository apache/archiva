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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva" uri="http://archiva.apache.org" %>

<html>
<head>
  <title>Administration - Proxy Connectors</title>
  <ww:head/>
</head>

<body>

<h1>Administration - Proxy Connectors</h1>

<c:url var="iconDeleteUrl" value="/images/icons/delete.gif"/>
<c:url var="iconEditUrl" value="/images/icons/edit.png"/>
<c:url var="iconCreateUrl" value="/images/icons/create.png"/>
<c:url var="iconUpUrl" value="/images/icons/up.gif"/>
<c:url var="iconDownUrl" value="/images/icons/down.gif"/>

<div id="contentArea">

<ww:actionerror/>
<ww:actionmessage/>

<div style="float:right">
  <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
    <ww:url id="addProxyConnectorUrl" action="addProxyConnector"/>
    <ww:a href="%{addProxyConnectorUrl}" cssClass="create">
      <img src="<c:url value="/images/icons/create.png" />"/>
      Add
    </ww:a>
  </redback:ifAnyAuthorized>
</div>

<h2>Repository Proxy Connectors</h2>

<c:choose>
<c:when test="${empty (proxyConnectorMap)}">
  <strong>No Repository Proxy Connectors Defined.</strong>
</c:when>
<c:otherwise>

<div class="admin">

<c:forEach items="${proxyConnectorMap}" var="repository" varStatus="i">

<div class="proxyConfig">
  <div class="managedRepo">
    <img src="<c:url value="/images/archiva-splat-32.gif"/>"/>
    <p class="id">${repository.key}</p>
    <p class="name">${repoMap[repository.key].name}</p>
  </div>

  <c:set var="numberOfRepos" value="${fn:length(repository.value)}" />

  <c:forEach items="${repository.value}" var="connector" varStatus="pc">
  
  <c:choose>
    <c:when test='${(pc.index)%2 eq 0}'>
      <c:set var="rowColor" value="dark" scope="page"/>
    </c:when>
    <c:otherwise>
      <c:set var="rowColor" value="lite" scope="page"/>
    </c:otherwise>
  </c:choose>
  
  <div class="connector ${rowColor}"> 
    <div class="controls">
      <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
        <ww:url id="sortDownProxyConnectorUrl" action="sortDownProxyConnector">
          <ww:param name="target" value="%{'${connector.targetRepoId}'}"/>
          <ww:param name="source" value="%{'${connector.sourceRepoId}'}"/>
        </ww:url>
        <ww:url id="sortUpProxyConnectorUrl" action="sortUpProxyConnector">
          <ww:param name="target" value="%{'${connector.targetRepoId}'}"/>
          <ww:param name="source" value="%{'${connector.sourceRepoId}'}"/>
        </ww:url>
        <ww:url id="editProxyConnectorUrl" action="editProxyConnector">
          <ww:param name="target" value="%{'${connector.targetRepoId}'}"/>
          <ww:param name="source" value="%{'${connector.sourceRepoId}'}"/>
        </ww:url>
        <ww:url id="deleteProxyConnectorUrl" action="deleteProxyConnector" method="confirmDelete">
          <ww:param name="source" value="%{'${connector.sourceRepoId}'}"/>
          <ww:param name="target" value="%{'${connector.targetRepoId}'}"/>
        </ww:url>
        <c:if test="${pc.count > 1}">
          <ww:a href="%{sortUpProxyConnectorUrl}" cssClass="up" title="Move Proxy Connector Up">
            <img src="${iconUpUrl}"/>
          </ww:a>
        </c:if>
        <c:if test="${pc.count < numberOfRepos}">
          <ww:a href="%{sortDownProxyConnectorUrl}" cssClass="down" title="Move Proxy Connector Down">
            <img src="${iconDownUrl}"/>
          </ww:a>
        </c:if>
        <ww:a href="%{editProxyConnectorUrl}" cssClass="edit" title="Edit Proxy Connector">
          <img src="${iconEditUrl}"/>
        </ww:a>
        <ww:a href="%{deleteProxyConnectorUrl}" cssClass="delete" title="Delete Proxy Connector">
          <img src="${iconDeleteUrl}"/>
        </ww:a>
      </redback:ifAnyAuthorized>
    </div>

    <h4>Proxy Connector</h4>
    
    <div class="remoteRepo">
      <img src="<c:url value="/images/archiva-world.png"/>"/>
      <p class="id">${connector.targetRepoId}</p>
      <p class="name">${repoMap[connector.targetRepoId].name}</p>
      <p class="url"><a href="${repoMap[connector.targetRepoId].url}">${repoMap[connector.targetRepoId].url}</a></p>
    </div>
    
    <a class="expand" href="#" onclick="Effect.toggle('proxySettings_${connector.sourceRepoId}_${connector.targetRepoId}','slide'); return false;">Expand</a>
    <table class="settings" style="display: none;" id="proxySettings_${connector.sourceRepoId}_${connector.targetRepoId}">
      <tr>
        <th nowrap="nowrap">Network Proxy:</th>
        <td>
          <c:choose>
            <c:when test="${empty (connector.proxyId)}">
              <span class="directConnection">(Direct Connection)</span>
            </c:when>
            <c:otherwise>
              <ww:url id="editProxyIdUrl" action="editNetworkProxy">
                <ww:param name="proxyid" value="%{'${connector.proxyId}'}"/>
              </ww:url>
              <ww:a href="%{editProxyIdUrl}" cssClass="edit" title="Edit Network Proxy">
                ${connector.proxyId}
                <img src="${iconEditUrl}"/>
              </ww:a>
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
      <tr>
        <th>Policies:</th>
        <td nowrap="nowrap">
          <table class="policies">
            <c:forEach items="${connector.policies}" var="policies">
              <tr>
                <th>${policies.key}</th>
                <td>${policies.value}</td>
              </tr>
            </c:forEach>
          </table>
        </td>
      </tr>

      <c:if test="${not (empty (connector.whiteListPatterns))}">
        <tr>
          <th nowrap="nowrap">White List:</th>
          <td nowrap="nowrap">
            <c:forEach items="${connector.whiteListPatterns}" var="pattern">
              <p><code>"${pattern}"</code></p>
            </c:forEach>
          </td>
        </tr>
      </c:if>

      <c:if test="${not (empty (connector.blackListPatterns))}">
        <tr>
          <th nowrap="nowrap">Black List:</th>
          <td>
            <c:forEach items="${connector.blackListPatterns}" var="pattern">
              <p><code>"${pattern}"</code></p>
            </c:forEach>
          </td>
        </tr>
      </c:if>

      <c:if test="${not (empty (connector.properties))}">
        <tr>
          <th>Properties:</th>
          <td>
            <table class="props">
              <c:forEach items="${connector.properties}" var="prop">
                <tr>
                  <th>${prop.key}</th>
                  <td>${prop.value}</td>
                </tr>
              </c:forEach>
            </table>
          </td>
        </tr>
      </c:if>
    </table>
  </div> <%-- connector --%>

</c:forEach>
</div> <%-- proxyConfig --%>
</c:forEach>
</div> <%-- admin --%>
</c:otherwise>
</c:choose>

</div>

</body>
</html>
