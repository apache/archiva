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

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<div id="companyLogo">
  <s:set name="organisationLogo" value="organisationLogo"/>
  <c:choose>
    <c:when test="${!empty (organisationLogo)}">
      <s:set name="organisationUrl" value="organisationUrl"/>
      <c:choose>
        <c:when test="${!empty (organisationUrl)}">
          <a href="${organisationUrl}">
            <img src="${organisationLogo}" title="${organisationName}"/>
          </a>
        </c:when>
        <c:otherwise>
          <img src="${organisationLogo}" title="${organisationName}"/>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <my:currentWWUrl action="index" namespace="/">
        <img src="<c:url value='/images/archiva.png' />"/>
      </my:currentWWUrl>
    </c:otherwise>
  </c:choose>
</div>