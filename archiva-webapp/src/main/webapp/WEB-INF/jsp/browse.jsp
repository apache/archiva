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
<%@ taglib prefix="pss" uri="plexusSecuritySystem" %>

<html>
<head>
  <title>Browse Repository</title>
  <ww:head/>
</head>

<body>

<h1>Browse Repository</h1>
<div id="contentArea">
  <div id="nameColumn">
    <h2>Groups</h2>
    <ul>
      <ww:set name="groups" value="groups"/>
      <c:forEach items="${groups}" var="groupId">
        <c:set var="url">
          <ww:url action="browseGroup" namespace="/">
            <ww:param name="groupId" value="%{'${groupId}'}"/>
          </ww:url>
        </c:set>
        <li><a href="${url}">${groupId}/</a></li>
      </c:forEach>
    </ul>
  </div>



  <%-- TODO: later, when supported in metadata
  <div id="categoryColumn">
    <h2>Category</h2>
    <table>
      <tr>
        <td>
          <a href="#">Java</a>
        </td>
      </tr>
      <tr>
        <td>
          <a href="#">Ruby</a>
        </td>
      </tr>
    </table>
  </div>

  <h2>Labels</h2>

  <div id="labels">
    <p>
      <a href="#">jdo</a>
      <a href="#">j2ee</a>
      <a href="#">maven</a>
    </p>
  </div>
  --%>
</div>

</body>
</html>
