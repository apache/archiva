<%--
  Created by IntelliJ IDEA.
  User: jesse
  Date: Sep 18, 2006
  Time: 1:48:21 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

    <s:set name="availableRoles" value="#availableRoles"/>
    <s:form action="user" name="grantRole" method="post" namespace="/security/admin">
      <s:hidden name="principal">${username}"</s:hidden>
      <s:select name="roleName" list="availableRoles" labelposition="top" />
      <s:submit value="%{getText('grant')}" />
    </s:form>
