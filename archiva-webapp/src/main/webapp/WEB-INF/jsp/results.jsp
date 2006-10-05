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

<%@ taglib uri="/webwork" prefix="ww" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<html>
<head>
  <title>Search Results</title>
  <ww:head />
</head>

<body>

<h1>Search</h1>

<div id="contentArea">
  <div id="searchBox">
    <%@ include file="/WEB-INF/jsp/include/quickSearchForm.jspf" %>
  </div>

    <h1>Results</h1>
    <div id="resultsBox">
        <ww:set name="searchResults" value="searchResults" />
        <c:forEach items="${searchResults}" var="record" varStatus="i">


          <h3 class="artifact-title">
            <my:showArtifactTitle groupId="${record.groupId}" artifactId="${record.artifactId}"
                                  version="${record.version}"/>
          </h3>

          <p>
          <my:showArtifactLink groupId="${record.groupId}" artifactId="${record.artifactId}" 
                               version="${record.version}" versions="${record.versions}"/>

              <%-- TODO: hits
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
          </p>
        </c:forEach>
    </div>
</div>
</body>
</html>
