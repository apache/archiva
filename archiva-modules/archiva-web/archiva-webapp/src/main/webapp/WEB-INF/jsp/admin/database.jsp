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

<%@ taglib prefix="ww" uri="/webwork"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>
<%@ taglib prefix="archiva" uri="http://archiva.apache.org" %>

<html>
<head>
<title>Administration - Database</title>
<ww:head />
</head>

<body>

<h1>Administration - Database</h1>

<div id="contentArea">

<ww:actionerror /> 
<ww:actionmessage /> 

<c:url var="iconDeleteUrl" value="/images/icons/delete.gif" /> 
<c:url var="iconCreateUrl" value="/images/icons/create.png" /> 
   
<div class="admin">

<h2>Database - Unprocessed Artifacts Scanning</h2>

  <ww:form method="post" action="database!updateSchedule" 
             namespace="/admin" validate="false" theme="simple">
    <table>
      <ww:textfield name="cron" label="Cron" size="40" theme="xhtml" />
      <tr>
        <td colspan="2">
          <ww:submit value="Update Cron" />
        </td>
      </tr>
    </table>                 
  </ww:form>
  
  <ww:form action="updateDatabase" theme="simple">
    <ww:submit value="Update Database Now"/>
  </ww:form>

<h2>Database - Unprocessed Artifacts Scanning</h2>

<c:choose>
  <c:when test="${empty(unprocessedConsumers)}">
    <%-- No Consumers. Eeek! --%>
    <strong>There are no consumers for unprocessed artifacts.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the consumers. --%>

    <ww:form method="post" action="database!updateUnprocessedConsumers" 
             namespace="/admin" validate="false" theme="simple">
    <table class="consumers">
      <tr>
        <th>&nbsp;</th>
        <th>Enabled?</th>
        <th>ID</th>
        <th>Description</th>
      </tr>
      <c:forEach items="${unprocessedConsumers}" var="consumer" varStatus="i">
        <c:choose>
          <c:when test='${(i.index)%2 eq 0}'>
            <c:set var="bgcolor" value="even" scope="page" />
          </c:when>
          <c:otherwise>
            <c:set var="bgcolor" value="odd" scope="page" />
          </c:otherwise>
        </c:choose>

        <tr>
          <td class="${bgcolor}">
            <input type="checkbox" name="enabledUnprocessedConsumers" theme="simple" value="${consumer.id}" <c:if test="${consumer.enabled}">checked</c:if> />
          </td>
          <td class="${bgcolor}">
            <c:if test="${consumer.enabled}">
              <strong>enabled</strong>
            </c:if>
          </td>
          <td class="${bgcolor}">
            <code>${consumer.id}</code>
          </td>
          <td class="${bgcolor}">${consumer.description}</td>
        </tr>
      </c:forEach>
      <tr>
        <td colspan="4">
          <ww:submit value="Update Consumers" />
        </td>
      </tr>
    </table>
    </ww:form>

  </c:otherwise>
</c:choose>

<h2>Database - Artifact Cleanup Scanning</h2>

<c:choose>
  <c:when test="${empty(cleanupConsumers)}">
    <%-- No Consumers. Eeek! --%>
    <strong>There are no consumers for artifact cleanup.</strong>
  </c:when>
  <c:otherwise>
    <%-- Display the consumers. --%>

    <ww:form method="post" action="database!updateCleanupConsumers" 
             namespace="/admin" validate="false" theme="simple">
    <table class="consumers">
      <tr>
        <th>&nbsp;</th>
        <th>Enabled?</th>
        <th>ID</th>
        <th>Description</th>
      </tr>
      <c:forEach items="${cleanupConsumers}" var="consumer" varStatus="i">
        <c:choose>
          <c:when test='${(i.index)%2 eq 0}'>
            <c:set var="bgcolor" value="even" scope="page" />
          </c:when>
          <c:otherwise>
            <c:set var="bgcolor" value="odd" scope="page" />
          </c:otherwise>
        </c:choose>

        <tr>
          <td class="${bgcolor}">
            <input type="checkbox" name="enabledCleanupConsumers" theme="simple" value="${consumer.id}" <c:if test="${consumer.enabled}">checked</c:if> />
          </td>
          <td class="${bgcolor}">
            <c:if test="${consumer.enabled}">
              <strong>enabled</strong>
            </c:if>
          </td>
          <td class="${bgcolor}">
            <code>${consumer.id}</code>
          </td>
          <td class="${bgcolor}">${consumer.description}</td>
        </tr>
      </c:forEach>
      <tr>
        <td colspan="4">
          <ww:submit value="Update Consumers" />
        </td>
      </tr>
    </table>
    </ww:form>

  </c:otherwise>
</c:choose>


</div>
</div>
</body>
</html>
