<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
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
<h2>Proxied Repositories</h2>

<ww:set name="proxiedRepositories" value="proxiedRepositories"/>
<c:if test="${empty(proxiedRepositories)}">
  <strong>There are no proxied repositories configured yet.</strong>
</c:if>
<c:forEach items="${proxiedRepositories}" var="repository" varStatus="i">
<div>
<div style="float: right">
    <%-- TODO replace with icons --%>
  <a href="<ww:url action="editProxiedRepository" method="input"><ww:param name="repoId" value="%{'${repository.id}'}" /></ww:url>">Edit
    Repository</a> | <a
    href="<ww:url action="deleteProxiedRepository" method="input"><ww:param name="repoId" value="%{'${repository.id}'}" /></ww:url>">Delete
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
    <th>URL</th>
    <td><a href="${repository.url}">${repository.url}</a></td>
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
    <th>Snapshots</th>
    <td>
        <%-- TODO! turn into a tag --%>
      <c:choose>
        <c:when test="${repository.snapshotsPolicy == 'disabled'}">
          Disabled
        </c:when>
        <c:when test="${repository.snapshotsPolicy == 'always'}">
          Updated every request
        </c:when>
        <c:when test="${repository.snapshotsPolicy == 'hourly'}">
          Updated hourly
        </c:when>
        <c:when test="${repository.snapshotsPolicy == 'daily'}">
          Updated daily
        </c:when>
        <c:when test="${repository.snapshotsPolicy == 'interval'}">
          Updated every ${repository.snapshotsInterval} minutes
        </c:when>
      </c:choose>
    </td>
  </tr>
  <tr>
    <th>Releases</th>
    <td>
        <%-- TODO! turn into a tag --%>
      <c:choose>
        <c:when test="${repository.releasesPolicy == 'disabled'}">
          Disabled
        </c:when>
        <c:when test="${repository.releasesPolicy == 'always'}">
          Updated every request
        </c:when>
        <c:when test="${repository.releasesPolicy == 'hourly'}">
          Updated hourly
        </c:when>
        <c:when test="${repository.releasesPolicy == 'daily'}">
          Updated daily
        </c:when>
        <c:when test="${repository.releasesPolicy == 'interval'}">
          Updated every ${repository.releasesInterval} minutes
        </c:when>
      </c:choose>
    </td>
  </tr>
  <tr>
    <th>Proxied through</th>
    <td>
        <%-- TODO: this is the hard way! would be nice if there was a ref in the model so it was directly linked --%>
        ${repositoriesMap[repository.managedRepository].name}
      (<code>${repositoriesMap[repository.managedRepository].id}</code>)
    </td>
  </tr>
  <tr>
    <th>Use HTTP Proxy</th>
    <td class="${repository.useNetworkProxy ? 'doneMark' : 'errorMark'}"></td>
  </tr>
  <tr>
    <th>Cache Failures</th>
    <td class="${repository.cacheFailures ? 'doneMark' : 'errorMark'}"></td>
  </tr>
  <tr>
    <th>Fail Whole Group</th>
    <td class="${repository.hardFail ? 'doneMark' : 'errorMark'}"></td>
  </tr>
</table>
</div>
</c:forEach>

<p>
  <a href="<ww:url action="addProxiedRepository" method="input" />">Add Repository</a>
</p>
</div>

</body>
</html>