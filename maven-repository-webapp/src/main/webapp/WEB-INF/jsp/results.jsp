<%@ taglib uri="/webwork" prefix="ww" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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

<html>
<head>
  <title>Search Results</title>
  <ww:head />
</head>

<body>

<h1>Search Results</h1>

<div id="contentArea">
  <div id="searchBox">
    <%@ include file="/WEB-INF/jsp/include/quickSearchForm.jspf" %>

    <div id="resultsBox">
      <table class="bodyTable">
        <tr class="a">
          <th>Group</th>
          <th>Artifact</th>
          <th>Version</th>
          <%-- TODO
                    <th>Hits</th>
                    <th></th>
          --%>
        </tr>
        <ww:set name="searchResults" value="searchResults" />
        <c:forEach items="${searchResults}" var="result" varStatus="i">
          <tr class="${i.index % 2 == 0 ? "b" : "a"}">
            <td><c:out value="${result.artifact.groupId}" /></td>
            <td><c:out value="${result.artifact.artifactId}" /></td>
            <td><c:out value="${result.artifact.version}" /></td>
              <%-- TODO: hits
            <td>
              <table border="1px" width="100%" cellspacing="0">
                <c:forEach items="${result.fieldMatchesEntrySet}" var="entry">
                  <tr>
                    <td valign="top" width="15%" align="right"><c:out value="${entry.key}"/></td>
                    <td valign="top">
                      <c:forEach items="${entry.value}" var="item">
                        <c:out value="${item}" />
                      </c:forEach>
                      <br/>
                    </td>
                  </tr>
                </c:forEach>
              </table>
            </td>
              <td>

                <code>org.apache.maven</code>
                (package)
                <br/>
                <code>org.apache.maven.model</code>
                (package)
              </td>
              <td>
                <a href="artifact.html">Details</a>
              </td>
              --%>
          </tr>
        </c:forEach>
      </table>
    </div>
  </div>
</div>
</body>
</html>
