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

<%@ taglib uri="/webwork" prefix="ww" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<html>
<head>
  <title>Search Results</title>
  <ww:head/>
</head>

<body>

<h1>Search</h1>

<div id="contentArea">
  <div id="searchBox">
    <%@ include file="/WEB-INF/jsp/include/quickSearchForm.jspf" %>
  </div>

  <h1>Results</h1>

  <div id="resultsBox">
    <c:choose>

      <%-- search was made from the indices --%>
      <c:when test="${databaseResults == null}">
        <p>Hits: ${fn:length(results.hits)} of ${results.totalHits}</p>

        <c:choose>
          <c:when test="${empty results.hits}">
            <p>No results</p>
          </c:when>
          <c:otherwise>
            <c:forEach items="${results.hits}" var="record" varStatus="i">
              <c:choose>
                <c:when test="${not empty (record.groupId)}">
                  <h3 class="artifact-title">
                    <my:showArtifactTitle groupId="${record.groupId}" artifactId="${record.artifactId}"
                                          version="${record.version}"/>
                  </h3>
                  <p>
                    <my:showArtifactLink groupId="${record.groupId}" artifactId="${record.artifactId}"
                                         version="${record.version}" versions="${record.versions}"/>
                  </p>
                </c:when>
                <c:otherwise>
                  <p>
                    <c:url var="hiturl" value="/repository/${record.url}" />
                    <a href="${hiturl}">${record.urlFilename}</a>
                  </p>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </c:when>

      <%-- search was made from the database (find artifact)--%>
      <c:otherwise>
        <p>Hits: ${fn:length(databaseResults)}</p>

        <c:choose>
          <c:when test="${empty databaseResults}">
            <p>No results</p>
          </c:when>
          <c:otherwise>
            <c:forEach items="${databaseResults}" var="artifactModel" varStatus="i">
              <c:choose>
                <c:when test="${not empty (artifactModel.groupId)}">
                  <h3 class="artifact-title">
                    <my:showArtifactTitle groupId="${artifactModel.groupId}" artifactId="${artifactModel.artifactId}"
                                          version="${artifactModel.version}"/>
                  </h3>
                  <p>
                    <my:showArtifactLink groupId="${artifactModel.groupId}" artifactId="${artifactModel.artifactId}"
                                         version="${artifactModel.version}" versions="${artifactModel.versions}"/>
                  </p>
                </c:when>
                <c:otherwise>
                  <p>
                    <c:url var="hiturl" value="/repository/${artifactModel.repositoryId}" />
                    <a href="${hiturl}">${artifactModel.repositoryId}</a>
                  </p>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </c:otherwise>
        </c:choose>

      </c:otherwise>
    </c:choose>
  </div>
</div>
</body>
</html>
