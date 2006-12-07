<%@ taglib uri="/webwork" prefix="ww" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<ww:set name="companyLogo" value="companyLogo"/>
<c:if test="${!empty(companyLogo)}">
  <ww:set name="companyUrl" value="companyUrl"/>
  <c:choose>
    <c:when test="${!empty(companyUrl)}">
      <a href="${companyUrl}">
        <img src="${companyLogo}" title="${companyName}" border="0" alt=""/>
      </a>
    </c:when>
    <c:otherwise>
      <img src="${companyLogo}" title="${companyName}" border="0" alt=""/>
    </c:otherwise>
  </c:choose>
</c:if>
