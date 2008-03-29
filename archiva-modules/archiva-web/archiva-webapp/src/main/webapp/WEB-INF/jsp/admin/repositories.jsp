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
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva" uri="http://maven.apache.org/archiva" %>

<html>
<head>
  <title>Administration - Repositories</title>
  <ww:head/>
</head>

<body>

<h1>Administration - Repositories</h1>

<div id="contentArea">

<ww:actionerror/>
<ww:actionmessage/>

<div class="admin">
<div class="controls">
  <redback:ifAuthorized permission="archiva-manage-configuration">
    <ww:url id="addRepositoryUrl" action="addRepository"/>
    <ww:a href="%{addRepositoryUrl}">
      <img src="<c:url value="/images/icons/create.png" />" alt="" width="16" height="16"/>
      Add
    </ww:a>
  </redback:ifAuthorized>
</div>
<h2>Managed Repositories</h2>

<c:choose>
<c:when test="${empty(managedRepositories)}">
  <%-- No Managed Repositories. --%>
  <strong>There are no managed repositories configured yet.</strong>
</c:when>
<c:otherwise>
<%-- Display the repositories. --%>

<c:forEach items="${managedRepositories}" var="repository" varStatus="i">
<c:choose>
  <c:when test='${(i.index)%2 eq 0}'>
    <c:set var="rowColor" value="dark" scope="page"/>
  </c:when>
  <c:otherwise>
    <c:set var="rowColor" value="lite" scope="page"/>
  </c:otherwise>
</c:choose>

<div class="repository ${rowColor}">

<div class="controls">
    <%-- TODO: make some icons --%>
  <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
    <ww:url id="editRepositoryUrl" action="editRepository">
      <ww:param name="repoid" value="%{'${repository.id}'}"/>
    </ww:url>
    <ww:url id="deleteRepositoryUrl" action="confirmDeleteRepository">
      <ww:param name="repoid" value="%{'${repository.id}'}"/>
    </ww:url>
    <ww:a href="%{editRepositoryUrl}">
      <img src="<c:url value="/images/icons/edit.png" />" alt="" width="16" height="16"/>
      Edit
    </ww:a>
    <ww:a href="%{deleteRepositoryUrl}">
      <img src="<c:url value="/images/icons/delete.gif" />" alt="" width="16" height="16"/>
      Delete
    </ww:a>
  </redback:ifAnyAuthorized>
</div>

<div style="float: left">
  <img src="<c:url value="/images/archiva-splat-32.gif"/>" alt="" width="32" height="32"/>
</div>

<h3 class="repository">${repository.name}</h3>

<table class="infoTable">
<tr>
  <th>Identifier</th>
  <td>
    <code>${repository.id}</code>
  </td>
</tr>
<tr>
  <th>Name</th>
  <td>
    <code>${repository.name}</code>
  </td>
</tr>
<tr>
  <th>Directory</th>
  <td>${repository.location}</td>
</tr>
<tr>
  <th>WebDAV URL</th>
  <td><a href="${baseUrl}/${repository.id}/">${baseUrl}/${repository.id}/</a></td>
</tr>
<tr>
  <th>Type</th>
    <%-- TODO: can probably just use layout appended to a key prefix in i18n to simplify this --%>
  <td>
    <c:choose>
      <c:when test="${repository.layout == 'default'}">
        Maven 2.x Repository
      </c:when>
      <c:otherwise>
        Maven 1.x Repository
      </c:otherwise>
    </c:choose>
  </td>
</tr>
<tr>
  <th>Releases Included</th>
  <td class="${repository.releases ? 'donemark' : 'errormark'} booleanIcon"> </td>
</tr>
<tr>
  <th>Snapshots Included</th>
  <td class="${repository.snapshots ? 'donemark' : 'errormark'} booleanIcon"> </td>
</tr>
<c:if test="${repository.snapshots}">
  <tr>
    <th>Delete Released Snapshots</th>
    <td class="${repository.deleteReleasedSnapshots ? 'donemark' : 'errormark'} booleanIcon"> </td>
  </tr>
  <tr>
    <th>Repository Purge By Days Older Than</th>
    <td>${repository.daysOlder}</td>
  </tr>
  <tr>
    <th>Repository Purge By Retention Count</th>
    <td>${repository.retentionCount}</td>
  </tr>
</c:if>
<tr>
  <th>Scanned</th>
  <td class="${repository.scanned ? 'donemark' : 'errormark'} booleanIcon"> </td>
</tr>
<c:if test="${repository.scanned}">
  <tr>
    <th>Scanning Cron</th>
    <td>${repository.refreshCronExpression}</td>
  </tr>
  <tr>
    <th>
      Actions
    </th>
    <td>
      <table>
        <tr>
          <td>
            <redback:ifAuthorized permission="archiva-run-indexer">
              <ww:form action="indexRepository" theme="simple">
                <ww:hidden name="repoid" value="%{'${repository.id}'}"/>
                <ww:submit value="Scan Repository Now"/>
              </ww:form>
            </redback:ifAuthorized>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <th>Stats</th>
    <td>
      <c:set var="stats" value="${repositoryStatistics[repository.id]}"/>
      <c:choose>
        <c:when test="${empty(stats)}">
          No Statistics Available.
        </c:when>
        <c:otherwise>
          <table>
            <tr>
              <th>Last Scanned</th>
              <td>${stats.whenGathered}</td>
            </tr>
            <tr>
              <th>Duration</th>
              <td>${stats.duration} ms</td>
            </tr>
            <tr>
              <th>Total File Count</th>
              <td>${stats.totalFileCount}
            </tr>
            <tr>
              <th>New Files Found</th>
              <td>${stats.newFileCount}
            </tr>
          </table>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
</c:if>
<tr>
  <th>POM Snippet</th>
  <td>
    <archiva:copy-paste-snippet object="${repository}" wrapper="toggle" />
  </td>
</tr>
</table>

</div>
</c:forEach>

</c:otherwise>
</c:choose>

<div class="controls">
  <redback:ifAuthorized permission="archiva-manage-configuration">
    <ww:url id="addRepositoryUrl" action="addRemoteRepository"/>
    <ww:a href="%{addRepositoryUrl}">
      <img src="<c:url value="/images/icons/create.png" />" alt="" width="16" height="16"/>
      Add
    </ww:a>
  </redback:ifAuthorized>
</div>
<h2>Remote Repositories</h2>

<c:choose>
  <c:when test="${empty(remoteRepositories)}">
    <%-- No Remote Repositories. --%>
    <strong>There are no remote repositories configured yet.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the repositories. --%>
    <c:forEach items="${remoteRepositories}" var="repository" varStatus="i">
      <c:choose>
        <c:when test='${(i.index)%2 eq 0}'>
          <c:set var="rowColor" value="dark" scope="page"/>
        </c:when>
        <c:otherwise>
          <c:set var="rowColor" value="lite" scope="page"/>
        </c:otherwise>
      </c:choose>

      <div class="repository ${rowColor}">

        <div class="controls">
          <redback:ifAnyAuthorized permissions="archiva-manage-configuration">
            <ww:url id="editRepositoryUrl" action="editRemoteRepository">
              <ww:param name="repoid" value="%{'${repository.id}'}"/>
            </ww:url>
            <ww:a href="%{editRepositoryUrl}">
              <img src="<c:url value="/images/icons/edit.png" />" alt="" width="16" height="16"/>
              Edit
            </ww:a>
            <ww:url id="deleteRepositoryUrl" action="confirmDeleteRemoteRepository">
              <ww:param name="repoid" value="%{'${repository.id}'}"/>
            </ww:url>
            <ww:a href="%{deleteRepositoryUrl}">
              <img src="<c:url value="/images/icons/delete.gif" />" alt="" width="16" height="16"/>
              Delete
            </ww:a>
          </redback:ifAnyAuthorized>
        </div>

        <div style="float: left">
          <img src="<c:url value="/images/archiva-world.png"/>" alt="" width="32" height="32"/>
        </div>

        <h3 class="repository">${repository.name}</h3>

        <table class="infoTable">
          <tr>
            <th>Identifier</th>
            <td>
              <code>${repository.id}</code>
            </td>
          </tr>
          <tr>
            <th>Name</th>
            <td>
              <code>${repository.name}</code>
            </td>
          </tr>
          <tr>
            <th>URL</th>
            <td>${repository.url}</td>
          </tr>
          <tr>
            <th>Type</th>
              <%-- TODO: can probably just use layout appended to a key prefix in i18n to simplify this --%>
            <td>
              <c:choose>
                <c:when test="${repository.layout == 'default'}">
                  Maven 2.x Repository
                </c:when>
                <c:otherwise>
                  Maven 1.x Repository
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </table>

      </div>
    </c:forEach>
  </c:otherwise>
</c:choose>

</div>

</div>

</body>
</html>
