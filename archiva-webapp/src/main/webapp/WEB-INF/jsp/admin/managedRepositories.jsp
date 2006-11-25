<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem" %>

<html>
<head>
  <title>Administration</title>
  <ww:head/>
</head>

<body>

<h1>Administration</h1>

<div id="contentArea">

<c:set var="urlbase">${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/repository/</c:set>

<div>
  <div style="float: right">
    <%-- TODO replace with icons --%>
    <pss:ifAuthorized permission="archiva-add-repository">
      <ww:url id="addRepositoryUrl" action="addRepository" method="input"/>
      <ww:a href="%{addRepositoryUrl}">Add Repository</ww:a>
    </pss:ifAuthorized>
  </div>
  <h2>Managed Repositories</h2>
</div>

<ww:set name="repositories" value="repositories"/>
<c:if test="${empty(repositories)}">
  <strong>There are no managed repositories configured yet.</strong>
</c:if>
<c:forEach items="${repositories}" var="repository" varStatus="i">
  <div>
    <div style="float: right">
      <ww:url id="editRepositoryUrl" action="editRepository" method="input">
        <ww:param name="repoId" value="%{'${repository.id}'}" />
      </ww:url>
      <ww:url id="deleteRepositoryUrl" action="deleteRepository" method="input">
        <ww:param name="repoId" value="%{'${repository.id}'}" />
      </ww:url>
      <%-- TODO replace with icons --%>
      <pss:ifAuthorized permission="archiva-edit-repository" resource="${repository.id}"><ww:a href="%{editRepositoryUrl}">Edit Repository</ww:a></pss:ifAuthorized>
      <pss:ifAuthorized permission="archiva-delete-repository" resource="${repository.id}"><ww:a href="%{deleteRepositoryUrl}">Delete Repository</ww:a></pss:ifAuthorized>
    </div>
    <h3>${repository.name}</h3>
    <table class="infoTable">
      <tr>
        <th>Identifier</th>
        <td>
          <code>${repository.id}</code>
        </td>
      </tr>
      <tr>
        <th>Directory</th>
        <td>${repository.directory}</td>
      </tr>
      <tr>
        <th>WebDAV URL</th>
        <td><a href="${urlbase}${repository.urlName}">${urlbase}${repository.urlName}</a></td>
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
        <th>Snapshots Included</th>
        <td class="${repository.includeSnapshots ? 'doneMark' : 'errorMark'} booleanIcon"> ${repository.includeSnapshots}</td>
      </tr>
      <tr>
        <th>Indexed</th>
        <td class="${repository.indexed ? 'doneMark' : 'errorMark'} booleanIcon"> ${repository.indexed}</td>
      </tr>
      <tr>
        <th>POM Snippet</th>
        <td><a href="#" onclick="Effect.toggle('repoPom${repository.id}','slide'); return false;">Show POM Snippet</a><br />
<pre class="pom" style="display: none;" id="repoPom${repository.id}"><code>&lt;project>
  ...
  &lt;distributionManagement>
    &lt;${repository.includeSnapshots ? 'snapshotRepository' : 'repository'}>
      &lt;id>${repository.id}&lt;/id>
      &lt;url>dav:${urlbase}${repository.urlName}&lt;/url><c:if test="${repository.layout != 'default'}">
      &lt;layout>${repository.layout}&lt;/layout></c:if>
    &lt;/${repository.includeSnapshots ? 'snapshotRepository' : 'repository'}>
  &lt;/distributionManagement>
  
  &lt;repositories>
    &lt;repository>
      &lt;id>${repository.id}&lt;/id>
      &lt;name>${repository.name}&lt;/name>
      &lt;url>${urlbase}${repository.urlName}&lt;/url><c:if test="${repository.layout != 'default'}">
      &lt;layout>${repository.layout}&lt;/layout></c:if>
      &lt;releases>
        &lt;enabled>${repository.includeSnapshots ? 'false' : 'true'}&lt;/enabled>
      &lt;/releases>
      &lt;snapshots>
        &lt;enabled>${repository.includeSnapshots ? 'true' : 'false'}&lt;/enabled>
      &lt;/snapshots>
    &lt;/repository>
  &lt;/repositories>
  ...
&lt;/project>
</code></pre>        
        </td>
      </tr>
    </table>
  </div>
</c:forEach>
</div>

</body>
</html>
