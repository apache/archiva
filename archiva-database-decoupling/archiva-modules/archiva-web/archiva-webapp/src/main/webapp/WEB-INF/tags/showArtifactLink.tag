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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="archiva" uri="http://archiva.apache.org" %>

<%@ attribute name="groupId" required="true" %>
<%@ attribute name="artifactId" %>
<%@ attribute name="version" %>
<%@ attribute name="classifier" %>
<%@ attribute name="scope" %>
<%@ attribute name="versions" type="java.util.List" %>
<%@ attribute name="repositoryId" %>

<span class="artifact-link">
  <a href="${pageContext.request.contextPath}/repository/${repositoryId}">${repositoryId}</a>
  <strong> : </strong>
  <archiva:groupIdLink var="${groupId}" includeTop="false" />
  
  <c:if test="${!empty (artifactId)}">    
    <c:set var="url">
      <ww:url action="browseArtifact" namespace="/">
        <ww:param name="groupId" value="%{'${groupId}'}"/>
        <ww:param name="artifactId" value="%{'${artifactId}'}"/>
      </ww:url>
    </c:set>
    <a href="${url}">${artifactId}</a>
  </c:if>
  | <strong>Version(s):</strong>
  <c:choose>
    <c:when test="${!empty (version)}">
      <c:set var="url">
        <ww:url action="showArtifact" namespace="/">
          <ww:param name="groupId" value="%{'${groupId}'}"/>
          <ww:param name="artifactId" value="%{'${artifactId}'}"/>
          <c:if test="${!empty (version)}">
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
  <c:if test="${!empty (scope)}">
    | <strong>Scope:</strong> ${scope}
  </c:if>
  <c:if test="${!empty (classifier)}">
    | <strong>Classifier:</strong> ${classifier}
  </c:if>  
</span>
