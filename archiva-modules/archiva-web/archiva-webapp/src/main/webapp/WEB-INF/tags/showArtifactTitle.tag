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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="groupId" required="true" %>
<%@ attribute name="artifactId" %>
<%@ attribute name="version" %>

  <span class="artifact-title">
    <c:set var="url">
      <c:choose>
        <c:when test="${!empty (version)}">
          <s:url action="showArtifact" namespace="/">
            <s:param name="groupId" value="%{#attr.groupId}"/>
            <s:param name="artifactId" value="%{#attr.artifactId}"/>
            <s:param name="version" value="%{#attr.version}"/>
          </s:url>
        </c:when>
        <c:otherwise>
          <s:url action="browseArtifact" namespace="/">
            <s:param name="groupId" value="%{#attr.groupId}"/>
            <s:param name="artifactId" value="%{#attr.artifactId}"/>
          </s:url>
        </c:otherwise>
      </c:choose>
    </c:set>
      <%-- TODO: showing the name and description would be nice, but that would require loading the POMs --%>
    <a href="${url}">${artifactId}</a>
  </span>
