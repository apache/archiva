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
<%@ taglib prefix="my" tagdir="/WEB-INF/tags" %>

<html>
<head>
  <title>Browse Repository</title>
  <ww:head/>
</head>

<body>

<%-- TODO: image by type
<img src="images/jar.png" width="100" height="100" alt="jar" style="float: left" />
--%>

<%-- TODO: download link
<div class="downloadButton">
  <a href="#">Download</a>
</div>
--%>

<ww:set name="model" value="model"/>
<h1>
  <c:choose>
    <c:when test="${empty(model.name)}">
      ${model.artifactId}
    </c:when>
    <c:otherwise>
      ${model.name}
    </c:otherwise>
  </c:choose>
</h1>

<div id="contentArea">
  <div id="tabs">
    <p>
      <c:set var="url">
        <ww:url action="showArtifact">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Info</my:currentWWUrl>
      <c:set var="url">
        <ww:url action="showArtifactDependencies">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Dependencies</my:currentWWUrl>
      <c:set var="url">
        <ww:url action="showArtifactDependees">
          <ww:param name="groupId" value="%{groupId}"/>
          <ww:param name="artifactId" value="%{artifactId}"/>
          <ww:param name="version" value="%{version}"/>
        </ww:url>
      </c:set>
      <my:currentWWUrl url="${url}">Depended On By</my:currentWWUrl>
      <%-- TODO:
          <a href="TODO">Mailing Lists</a>
      --%>
    </p>
  </div>

  <%-- TODO: perhaps using ajax? --%>
  <%-- TODO: panels? this is ugly as is! --%>
  <div id="tabArea">
    <c:choose>
      <c:when test="${dependencies != null}">
        <%@ include file="/WEB-INF/jsp/include/artifactDependencies.jspf" %>
      </c:when>
      <c:otherwise>
        <%@ include file="/WEB-INF/jsp/include/artifactInfo.jspf" %>
      </c:otherwise>
    </c:choose>
  </div>
</div>

</body>
</html>
