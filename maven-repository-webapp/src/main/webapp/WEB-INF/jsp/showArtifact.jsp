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

<html>
<head>
  <title>Browse Repository</title>
  <ww:head />
</head>

<body>

<%-- TODO: image by type
<img src="images/jar.png" width="100" height="100" alt="jar" style="float: left" />
--%>

<%-- TODO: download link
<div class="downloadButton">
  <a href="#">Download</a>
</div>
--%>

<ww:set name="model" value="model" />
<h1>
  <c:choose>
    <c:when test="${empty(model.name)}">
      ${model.artifactId}
    </c:when>
    <c:otherwise>
      ${model.name}
    </c:otherwise>
  </c:choose>
</h1>

<div id="contentArea">
<div id="tabs">
  <p>
    <strong>Info</strong>
    <%-- TODO: perhaps using ajax?
        <a href="TODO">Dependencies</a>
        <a href="TODO">Depended On</a>
        <a href="TODO">Mailing Lists</a>
        <a href="TODO">Developers</a>
        <a href="TODO">POM</a>
    --%>
  </p>
</div>

<div id="tabArea">
<p>
  <c:forTokens items="${model.groupId}" delims="." var="part">
    <c:choose>
      <c:when test="${empty(cumulativeGroup)}">
        <c:set var="cumulativeGroup" value="${part}" />
      </c:when>
      <c:otherwise>
        <c:set var="cumulativeGroup" value="${cumulativeGroup}/${part}" />
      </c:otherwise>
    </c:choose>
    <ww:url id="url" action="browseGroup" namespace="/">
      <ww:param name="groupId" value="%{'${cumulativeGroup}'}" />
    </ww:url>
    <a href="${url}">${part}</a> /
  </c:forTokens>
  <ww:url id="url" action="browseArtifact" namespace="/">
    <ww:param name="groupId" value="%{'${model.groupId}'}" />
    <ww:param name="artifactId" value="%{'${model.artifactId}'}" />
  </ww:url>
  <a href="${url}">${model.artifactId}</a> /
  <strong>${model.version}</strong>

  <!-- TODO: new versions?
    (<strong class="statusFailed">Newer version available:</strong>
    <a href="artifact.html">2.0.3</a>)
  -->
</p>

<p>${mode.description}</p>

<table>
  <tr>
    <th>Group ID</th>
    <td>${model.groupId}</td>
  </tr>
  <tr>
    <th>Artifact ID</th>
    <td>${model.artifactId}</td>
  </tr>
  <tr>
    <th>Version</th>
    <td>${model.version}</td>
  </tr>
  <%-- TODO: derivatives
    <tr>
      <th>Derivatives</th>
      <td>
        <a href="#">Source</a>
        |
        <a href="#">Javadoc</a>
      </td>
    </tr>
  --%>
  <c:if test="${model.parent != null}">
    <tr>
      <th>Parent</th>
      <td>
          ${model.parent.groupId} ${model.parent.artifactId} ${model.parent.version}
        <ww:url id="url" action="showArtifact" namespace="/">
          <ww:param name="groupId" value="%{'${model.parent.groupId}'}" />
          <ww:param name="artifactId" value="%{'${model.parent.artifactId}'}" />
          <ww:param name="version" value="%{'${model.parent.version}'}" />
        </ww:url>
        (<a href="${url}">View</a>)
      </td>
    </tr>
  </c:if>
  <%-- TODO: deployment timestamp
    <tr>
      <th>Deployment Date</th>
      <td>
        15 Jan 2006, 20:38:00 +1000
      </td>
    </tr>
  --%>
  <!-- TODO: origin
    <tr>
      <th>Origin</th>
      <td>
        <a href="TODO">Apache Repository</a>
      </td>
    </tr>
  -->
</table>

<c:if test="${model.organization != null || !empty(model.licenses)
    || model.issueManagement != null || model.ciManagement != null}">

  <h2>Other Details</h2>
  <table>
    <c:if test="${model.organization != null}">
      <tr>
        <th>Organisation</th>
        <td>
          <c:choose>
            <c:when test="${model.organization != null}">
              <a href="${model.organization.url}">${model.organization.name}</a>
            </c:when>
            <c:otherwise>
              ${model.organization.name}
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:if>
    <c:if test="${!empty(model.licenses)}">
      <c:forEach items="${model.licenses}" var="license">
        <tr>
          <th>License</th>
          <td>
            <c:choose>
              <c:when test="${!empty(license.url)}">
                <a href="${license.url}">${license.name}</a>
              </c:when>
              <c:otherwise>
                ${license.name}
              </c:otherwise>
            </c:choose>
          </td>
        </tr>
      </c:forEach>
    </c:if>
    <c:if test="${model.issueManagement != null}">
      <tr>
        <th>Issue Tracker</th>
        <td>
          <c:choose>
            <c:when test="${!empty(model.issueManagement.url)}">
              <a href="${model.issueManagement.url}">${model.issueManagement.system}</a>
            </c:when>
            <c:otherwise>
              ${model.issueManagement.system}
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:if>
    <c:if test="${model.ciManagement != null}">
      <tr>
        <th>Continuous Integration</th>
        <td>
          <c:choose>
            <c:when test="${!empty(model.ciManagement.url)}">
              <a href="${model.ciManagement.url}">${model.ciManagement.system}</a>
            </c:when>
            <c:otherwise>
              ${model.ciManagement.system}
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:if>
  </table>
</c:if>

<c:if test="${model.scm != null}">
  <h2>SCM</h2>
  <table>
    <c:if test="${!empty(model.scm.connection)}">
      <tr>
        <th>Connection</th>
        <td>
          <code>${model.scm.connection}</code>
        </td>
      </tr>
    </c:if>
    <c:if test="${!empty(model.scm.developerConnection)}">
      <tr>
        <th>Dev. Connection</th>
        <td>
          <code>${model.scm.developerConnection}</code>
        </td>
      </tr>
    </c:if>
    <c:if test="${!empty(model.scm.url)}">
      <tr>
        <th>Viewer</th>
        <td>
          <a href="${model.scm.url}">${model.scm.url}</a>
        </td>
      </tr>
    </c:if>
  </table>
</c:if>

</div>
</div>

</body>
</html>
