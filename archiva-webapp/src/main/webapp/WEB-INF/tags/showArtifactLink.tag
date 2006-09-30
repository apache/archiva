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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="groupId" required="true" %>
<%@ attribute name="artifactId" %>
<%@ attribute name="version" %>
<%@ attribute name="classifier" %>
<%@ attribute name="scope" %>
<%@ attribute name="versions" %>

<span style="font-size: x-small">
  <c:set var="cumulativeGroup" value=""/>
  <c:forTokens items="${groupId}" delims="." var="part" varStatus="i">
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
  <c:if test="${!empty(artifactId)}">
    <c:set var="url">
      <ww:url action="browseArtifact" namespace="/">
        <ww:param name="groupId" value="%{'${groupId}'}"/>
        <ww:param name="artifactId" value="%{'${artifactId}'}"/>
      </ww:url>
    </c:set>
    / <a href="${url}">${artifactId}</a>
  </c:if>
  | <strong>Version(s):</strong>
  <c:choose>
    <c:when test="${!empty(version)}">
      <c:set var="url">
        <ww:url action="showArtifact" namespace="/">
          <ww:param name="groupId" value="%{'${groupId}'}"/>
          <ww:param name="artifactId" value="%{'${artifactId}'}"/>
          <c:if test="${!empty(version)}">
            <ww:param name="version" value="%{'${version}'}"/>
          </c:if>
        </ww:url>
      </c:set>
      <a href="${url}">${version}</a>
    </c:when>
    <c:otherwise>
      <c:forEach items="${versions}" var="v" varStatus="i">
        <c:set var="url">
          <ww:url action="showArtifact" namespace="/">
            <ww:param name="groupId" value="%{'${groupId}'}"/>
            <ww:param name="artifactId" value="%{'${artifactId}'}"/>
            <ww:param name="version" value="%{'${v}'}"/>
          </ww:url>
        </c:set>
        <a href="${url}">${v}</a>
        <c:if test="${!i.last}">,</c:if>
      </c:forEach>
    </c:otherwise>
  </c:choose>
  <c:if test="${!empty(scope)}">
    | <strong>Scope:</strong> ${scope}
  </c:if>
  <c:if test="${!empty(classifier)}">
    | <strong>Classifier:</strong> ${classifier}
  </c:if>
</span>
