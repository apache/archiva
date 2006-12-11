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
<div>
  <div style="float: right">
    <%-- TODO replace with icons --%>
    <a href="<ww:url action="configure" />">Edit Configuration</a>
  </div>
  <h2>Configuration</h2>
</div>

<table class="infoTable">
  <tr>
    <th>Index Directory</th>
    <td>
      <ww:property value="indexPath"/>
    </td>
    <td></td>
  </tr>
  <tr>
    <th>Indexing Schedule</th>
    <td>
      <ww:property value="indexerCronExpression"/>
    </td>
    <%-- TODO: a "delete index and run now" operation should be here too (really clean, remove deletions that didn't get picked up) --%>
    <td>
      <pss:ifAuthorized permission="archiva-run-indexer">
        <a href="<ww:url action="runIndexer" />">Run Now</a>
      </pss:ifAuthorized>
    </td>
  </tr>
  <tr>
    <th>Last Indexing Time</th>
    <td>
      <ww:property value="lastIndexingTime"/>
    </td>
    <td></td>
  </tr>
</table>

<ww:set name="proxy" value="proxy"/>
<c:if test="${!empty(proxy.host)}">
  <h3>HTTP Proxy</h3>

  <table class="infoTable">
    <tr>
      <th>Host</th>
      <td>${proxy.host}</td>
    </tr>
    <tr>
      <th>Port</th>
      <td>${proxy.port}</td>
    </tr>
    <tr>
      <th>Username</th>
      <td>${proxy.username}</td>
    </tr>
  </table>
</c:if>

</body>
</html>
