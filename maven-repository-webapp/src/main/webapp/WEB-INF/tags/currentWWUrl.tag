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
<%@ attribute name="action" required="true" %>
<%@ attribute name="namespace" required="true" %>
<c:set var="currentUrl">
  <ww:url />
</c:set>
<c:set var="url">
  <ww:url action="${action}" namespace="${namespace}" />
</c:set>
<c:choose>
  <c:when test="${currentUrl == url}">
    <strong>
      <jsp:doBody />
    </strong>
  </c:when>
  <c:otherwise>
    <a href="${url}">
      <jsp:doBody />
    </a>
  </c:otherwise>
</c:choose>
