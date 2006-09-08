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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
  <title>Repository Health</title>
  <ww:head/>
</head>

<body>

<h1>Reports</h1>

<div id="contentArea">

<ww:set name="databases" value="databases"/>
<c:forEach items="${databases}" var="database">
<h2>Repository: ${database.repository.name}</h2>

<p>
  Status:
  <img src="<c:url value="/images/icon_error_sml.gif"/>" width="15" height="15" alt=""/>
    ${database.numFailures}
  <img src="<c:url value="/images/icon_warning_sml.gif"/>" width="15" height="15" alt=""/>
    ${database.numWarnings}
    <%-- TODO!
          (<a href="#">Repair all</a>)
    --%>
</p>

<%-- TODO! factor out common parts, especially artifact rendering tag --%>
<%-- TODO! paginate --%>
<c:if test="${!empty(database.reporting.artifacts)}">
  <h3>Artifacts</h3>
  <c:forEach items="${database.reporting.artifacts}" var="artifact" begin="0" end="2">
    <ul>
      <c:forEach items="${artifact.failures}" var="result">
        <li class="errorBullet">${result.reason}</li>
      </c:forEach>
      <c:forEach items="${artifact.warnings}" var="result">
        <li class="warningBullet">${result.reason}</li>
      </c:forEach>
    </ul>
    <p style="text-indent: 3em;">
          <span style="font-size: x-small">
            <%-- TODO! share with browse as a tag --%>
          <c:set var="cumulativeGroup" value=""/>
          <c:forTokens items="${artifact.groupId}" delims="." var="part">
            <c:choose>
              <c:when test="${empty(cumulativeGroup)}">
                <c:set var="cumulativeGroup" value="${part}"/>
              </c:when>
              <c:otherwise>
                <c:set var="cumulativeGroup" value="${cumulativeGroup}.${part}"/>
              </c:otherwise>
            </c:choose>
            <c:set var="url">
              <ww:url action="browseGroup" namespace="/">
                <ww:param name="groupId" value="%{'${cumulativeGroup}'}"/>
              </ww:url>
            </c:set>
            <a href="${url}">${part}</a> /
          </c:forTokens>
          <strong>${artifact.artifactId}</strong>
          | <strong>Version:</strong>
          <c:set var="url">
            <ww:url action="showArtifact" namespace="/">
              <ww:param name="groupId" value="%{'${artifact.groupId}'}"/>
              <ww:param name="artifactId" value="%{'${artifact.artifactId}'}"/>
              <c:if test="${!empty(artifact.version)}">
                <ww:param name="version" value="%{'${artifact.version}'}"/>
              </c:if>
            </ww:url>
          </c:set>
          <a href="${url}">${artifact.version}</a>
          <c:if test="${!empty(artifact.classifier)}">
            | <strong>Classifier:</strong> ${artifact.classifier}
          </c:if>
        </span>
    </p>
    <%-- TODO!
              <td>
                <a href="#">Repair</a>
              </td>
    --%>
  </c:forEach>
  <c:if test="${fn:length(database.reporting.artifacts) gt 3}">
    <p>
      <b>... more ...</b>
    </p>
  </c:if>
</c:if>
<c:if test="${!empty(database.reporting.metadata)}">
  <h3>Metadata</h3>
  <c:forEach items="${database.reporting.metadata}" var="metadata" begin="0" end="2">
    <ul>
      <c:forEach items="${metadata.failures}" var="result">
        <li class="errorBullet">${result.reason}</li>
      </c:forEach>
      <c:forEach items="${metadata.warnings}" var="result">
        <li class="warningBullet">${result.reason}</li>
      </c:forEach>
    </ul>
    <p style="text-indent: 3em;">
          <span style="font-size: x-small">
            <%-- TODO! share with browse as a tag --%>
          <c:set var="cumulativeGroup" value=""/>
          <c:forTokens items="${metadata.groupId}" delims="." var="part" varStatus="i">
            <c:choose>
              <c:when test="${empty(cumulativeGroup)}">
                <c:set var="cumulativeGroup" value="${part}"/>
              </c:when>
              <c:otherwise>
                <c:set var="cumulativeGroup" value="${cumulativeGroup}.${part}"/>
              </c:otherwise>
            </c:choose>
            <c:set var="url">
              <ww:url action="browseGroup" namespace="/">
                <ww:param name="groupId" value="%{'${cumulativeGroup}'}"/>
              </ww:url>
            </c:set>
            <a href="${url}">${part}</a>
            <c:if test="${!i.last}">
              /
            </c:if>
          </c:forTokens>
              <c:if test="${!empty(metadata.artifactId)}">
                <c:set var="url">
                  <ww:url action="browseArtifact" namespace="/">
                    <ww:param name="groupId" value="%{'${metadata.groupId}'}"/>
                    <ww:param name="artifactId" value="%{'${metadata.artifactId}'}"/>
                  </ww:url>
                </c:set>
                / <a href="${url}">${metadata.artifactId}</a>
              </c:if>
<c:if test="${!empty(metadata.version)}"> | <strong>Version:</strong>
  <c:set var="url">
    <ww:url action="showArtifact" namespace="/">
      <ww:param name="groupId" value="%{'${metadata.groupId}'}"/>
      <ww:param name="artifactId" value="%{'${metadata.artifactId}'}"/>
      <ww:param name="version" value="%{'${metadata.version}'}"/>
    </ww:url>
  </c:set>
  <a href="${url}">${metadata.version}</a>
</c:if>       
        </span>
    </p>
    <%-- TODO!
              <td>
                <a href="#">Repair</a>
              </td>
    --%>
  </c:forEach>
  <c:if test="${fn:length(database.reporting.metadata) gt 3}">
    <p>
      <b>... more ...</b>
    </p>
  </c:if>
</c:if>
</c:forEach>
</div>

</body>
</html>
