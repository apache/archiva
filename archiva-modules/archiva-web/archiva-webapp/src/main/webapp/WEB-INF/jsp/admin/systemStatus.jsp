<%@ page import="java.util.Date" %>
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
<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
  <title>Administration - System Status</title>
  <s:head/>
</head>

<body>

<h1>Administration - System Status</h1>

<div id="contentArea">

  <s:actionerror/>
  <s:actionmessage/>

  <h2>Queues</h2>

  <table>
    <tr>
      <th>Queue</th>
      <th>Size</th>
    </tr>
    <c:forEach var="queueEntry" items="${queues}">
      <c:set var="queue" value="${queueEntry.value.queueSnapshot}"/>
      <tr>
        <td>${queueEntry.key}</td>
        <td align="right">${fn:length(queue)}</td>
      </tr>
    </c:forEach>
  </table>

  <h2>Repository Scans Currently in Progress</h2>

  <c:choose>
  <c:when test="${!empty scanner.inProgressScans}">
  <table>
    <tr>
      <th>Repository</th>
      <th>Files processed</th>
      <th>New files</th>
    </tr>
    <c:forEach var="scan" items="${scanner.inProgressScans}">
      <tr>
        <td>${scan.repository.name} (${scan.repository.id})</td>
        <td align="right">${scan.stats.totalFileCount}</td>
        <td align="right">${scan.stats.newFileCount}</td>
      </tr>
      <tr>
        <td colspan="3">
          <table>
            <tr>
              <th>Name</th>
              <th>Total</th>
              <th>Average</th>
              <th>Invocations</th>
            </tr>
            <c:forEach var="entry" items="${scan.consumerTimings}">
              <tr>
                <c:set var="total" value="${scan.consumerCounts[entry.key]}"/>
                <td>${entry.key}</td>
                <td align="right">${entry.value}ms</td>
                <td align="right"><fmt:formatNumber value="${entry.value / total}" pattern="#"/>ms</td>
                <td align="right">${total}</td>
              </tr>
            </c:forEach>
          </table>
        </td>
      </tr>
    </c:forEach>
  </table>
  </c:when>
  <c:otherwise>
  <p>No scans in progress.</p>
  </c:otherwise>
  </c:choose>

  <h2>Caches</h2>

  <table>
    <tr>
      <th>Cache</th>
      <th>Size</th>
      <th>Hits</th>
      <th>Misses</th>
      <th>Hit Ratio</th>
      <th>&nbsp;</th>
    </tr>
    <c:forEach var="cacheEntry" items="${caches}">
      <c:set var="flushCacheUrl">
        <s:url action="flushCache">
          <s:param name="cacheKey">${cacheEntry.key}</s:param>
        </s:url>
      </c:set>
      <tr>
        <td>${cacheEntry.key}</td>
        <td align="right">${cacheEntry.value.statistics.size}</td>
        <td align="right">${cacheEntry.value.statistics.cacheHits}</td>
        <td align="right">${cacheEntry.value.statistics.cacheMiss}</td>
        <td align="right"><fmt:formatNumber value="${cacheEntry.value.statistics.cacheHitRate}" pattern="#%"/></td>
        <td><a href="${flushCacheUrl}">Flush</a></td>
      </tr>
    </c:forEach>
  </table>

  <h2>Memory Usage</h2>

  <p>${memoryStatus}</p>

  <h2>Current Time</h2>

  <p><%= new Date() %></p>
  
  </div>

</body>
</html>
