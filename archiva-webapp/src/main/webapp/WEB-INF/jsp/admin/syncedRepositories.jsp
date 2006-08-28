<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Administration</title>
  <ww:head/>
</head>

<body>

<h1>Administration</h1>

<div id="contentArea">
<h2>Synced Repositories</h2>

<ww:set name="syncedRepositories" value="syncedRepositories"/>
<c:if test="${empty(syncedRepositories)}">
  <strong>There are no synced repositories configured yet.</strong>
</c:if>
<c:forEach items="${syncedRepositories}" var="repository" varStatus="i">
  <div>
    <div style="float: right">
        <%-- TODO replace with icons --%>
      <a href="<ww:url action="editSyncedRepository" method="input"><ww:param name="repoId" value="%{'${repository.id}'}" /></ww:url>">Edit
        Repository</a> | <a
        href="<ww:url action="deleteSyncedRepository" method="input"><ww:param name="repoId" value="%{'${repository.id}'}" /></ww:url>">Delete
      Repository</a>
    </div>
    <h3>${repository.name}</h3>
    <table>
      <tr>
        <th>Identifier</th>
        <td>
          <code>${repository.id}</code>
        </td>
      </tr>
      <tr>
        <th>Method</th>
        <td>${repository.method}</td>
      </tr>
      <c:choose>
        <c:when test="${repository.method == 'cvs'}">
          <tr>
            <th>CVS Root</th>
            <td>${repository.properties['cvsRoot']}</td>
          </tr>
        </c:when>
        <c:when test="${repository.method == 'svn'}">
          <tr>
            <th>Subversion URL</th>
            <td>${repository.properties['svnUrl']}</td>
          </tr>
          <tr>
            <th>Subversion Username</th>
            <td>${repository.properties['username']}</td>
          </tr>
        </c:when>
        <c:when test="${repository.method == 'rsync'}">
          <tr>
            <th>Rsync Host</th>
            <td>${repository.properties['rsyncHost']}</td>
          </tr>
          <tr>
            <th>Rsync Directory</th>
            <td>${repository.properties['rsyncDirectory']}</td>
          </tr>
          <tr>
            <th>Rsync Method</th>
            <td>
              <c:choose>
                <c:when test="${repository.properties['rsyncMethod'] == 'rsync'}">
                  Anonymous
                </c:when>
                <c:when test="${repository.properties['rsyncMethod'] == 'ssh'}">
                  SSH
                </c:when>
              </c:choose>
            </td>
          </tr>
          <tr>
            <th>Username</th>
            <td>${repository.properties['username']}</td>
          </tr>
        </c:when>
        <c:when test="${repository.method == 'file'}">
          <tr>
            <th>Directory</th>
            <td>${repository.properties['directory']}</td>
          </tr>
        </c:when>
      </c:choose>
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
        <th>Synced to</th>
        <td>
            <%-- TODO: this is the hard way! would be nice if there was a ref in the model so it was directly linked --%>
            ${repositoriesMap[repository.managedRepository].name}
          (<code>${repositoriesMap[repository.managedRepository].id}</code>)
        </td>
      </tr>
      <tr>
        <th>Schedule</th>
        <td>${repository.cronExpression}</td>
      </tr>
    </table>
  </div>
</c:forEach>

<p>
  <a href="<ww:url action="addSyncedRepository" method="input" />">Add Repository</a>
</p>
</div>

</body>
</html>