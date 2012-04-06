<%--
  ~ Copyright 2005-2006 The Codehaus.
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

<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/redback/taglib-1.0" prefix="redback" %>

<span class="securityLinks">

<c:choose>
  <c:when test="${sessionScope.securitySession.authenticated != true}">
    <s:url id="loginUrl" action="login" namespace="/security" includeParams="none"/>  
    <s:url id="registerUrl" action="register" namespace="/security" includeParams="none"/>
    <s:a id="loginLink" href="%{loginUrl}"><s:text name="login"/></s:a><redback:isNotReadOnlyUserManager> - <s:a id="registerLink" href="%{registerUrl}"><s:text name="register"/></s:a></redback:isNotReadOnlyUserManager>
  </c:when>
  <c:otherwise>
    <s:url id="logoutUrl" action="logout" namespace="/security" includeParams="none"/>
    <s:url id="accountUrl" action="account" namespace="/security" includeParams="none" />
    
    <s:text name="current.user"/>
    <c:choose>
      <c:when test="${sessionScope.securitySession.user != null}">
        <span class="fullname"><s:a href="%{accountUrl}" cssClass="edit"><c:out value="${sessionScope.securitySession.user.fullName}" /></s:a></span>
        (<span class="username"><c:out value="${sessionScope.securitySession.user.username}" /></span>)
      </c:when>
      <c:otherwise>
        <span class="fullname"><s:text name="%{unknown.user}"/></span>
      </c:otherwise>
    </c:choose>
    
    <redback:isNotReadOnlyUserManager>
    - <s:a id="editUserLink" href="%{accountUrl}" cssClass="edit"><s:text name="edit.details"/></s:a>
    </redback:isNotReadOnlyUserManager>
    - <s:a id="logoutLink" href="%{logoutUrl}" cssClass="logout"><s:text name="logout"/></s:a>
    
    <c:if test="${sessionScope.passwordExpirationNotification != null}">
    - <s:text name="notify.password.expiration"/> ${sessionScope.passwordExpirationNotification}
    </c:if>
  </c:otherwise>
</c:choose>

</span>
