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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="pss" uri="/plexusSecuritySystem" %>

<html>
<head>
  <ww:set name="reports" value="reports"/>
  <ww:set name="reportGroup" value="reportGroup"/>
  <title>Report: ${reports[reportGroup].name}</title>
  <ww:head/>
</head>

<body>

<h1>Reports</h1>

<div id="contentArea">

<pss:ifAnyAuthorized permissions="generate-reports">
  <ww:form action="reports" namespace="/admin">
    <ww:select list="reports" label="Report" name="reportGroup" onchange="document.reports.submit();"/>
    <ww:select list="configuration.repositories" listKey="id" listValue="name" label="Repository" headerKey="-"
               headerValue="(All repositories)" name="repositoryId" onchange="document.reports.submit();"/>
    <ww:select list="reports[reportGroup].reports" label="Filter" headerKey="-" headerValue="(All Problems)"
               name="filter" onchange="document.reports.submit();"/>
    <ww:submit value="Get Report"/>
  </ww:form>
</pss:ifAnyAuthorized>

<ww:set name="databases" value="databases"/>
<c:forEach items="${databases}" var="database">
<div>
<div style="float: right">
    <%-- TODO!
  <a href="#">Repair all</a>
  |
    --%>
  <c:choose>
    <c:when test="${!database.inProgress}">
      <pss:ifAuthorized permission="generate-reports">
        <ww:url id="regenerateReportUrl" action="runReport" namespace="/admin">
          <ww:param name="repositoryId">${database.repository.id}</ww:param>
          <ww:param name="reportGroup" value="reportGroup"/>
        </ww:url>
        <ww:a href="%{regenerateReportUrl}">Regenerate Report</ww:a>
      </pss:ifAuthorized>
    </c:when>
    <c:otherwise>
      <!-- TODO: would be good to have a generic task/job mechanism that tracked progress and ability to run
      concurrently -->
      <span style="color: gray;">Report in progress</span>
    </c:otherwise>
  </c:choose>
</div>
<h2>Repository: ${database.repository.name}</h2>

<p>
  <c:choose>
    <c:when test="${!empty(database.reporting.lastModified)}">
      Status:
      <img src="<c:url value="/images/icon_error_sml.gif"/>" width="15" height="15" alt=""/>
      ${database.numFailures}
      <img src="<c:url value="/images/icon_warning_sml.gif"/>" width="15" height="15" alt=""/>
      ${database.numWarnings}
      <img src="<c:url value="/images/icon_info_sml.gif"/>" width="15" height="15" alt=""/>
      ${database.numNotices}

      <span style="font-size: x-small">
        <%-- TODO! use better formatting here --%>
        Last updated: ${database.reporting.lastModified},
        execution time: <fmt:formatNumber maxFractionDigits="0" value="${database.reporting.executionTime / 60000}"/> minutes
        <fmt:formatNumber maxFractionDigits="0" value="${(database.reporting.executionTime / 1000) % 60}"/> seconds
      </span>
    </c:when>
    <c:otherwise>
      <b>
        This report has not yet been generated. <a href="${url}">Generate Report</a>
      </b>
    </c:otherwise>
  </c:choose>
</p>

  <%-- TODO need to protect iterations against concurrent modification exceptions by cloning the lists synchronously --%>
  <%-- TODO! factor out common parts, especially artifact rendering tag --%>
  <%-- TODO! paginate (displaytag?) --%>
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
      <c:forEach items="${artifact.notices}" var="result">
        <li class="infoBullet">${result.reason}</li>
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
<c:if test="${!empty(database.metadataWithProblems)}">
  <h3>Metadata</h3>
  <c:forEach items="${database.metadataWithProblems}" var="metadata" begin="0" end="2">
    <ul>
      <c:forEach items="${metadata.failures}" var="result">
        <li class="errorBullet">${result.reason}</li>
      </c:forEach>
      <c:forEach items="${metadata.warnings}" var="result">
        <li class="warningBullet">${result.reason}</li>
      </c:forEach>
      <c:forEach items="${metadata.notices}" var="result">
        <li class="infoBullet">${result.reason}</li>
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
  <c:if test="${fn:length(database.metadataWithProblems) gt 3}">
    <p>
      <b>... more ...</b>
    </p>
  </c:if>
</c:if>
</div>
</c:forEach>
</div>

</body>
</html>
