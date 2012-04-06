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

<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib prefix="redback" uri="/redback/taglib-1.0"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="role.model.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/rbacListNavigation.jsp" %>

<h2><s:text name="role.model.section.title"/></h2>

	<p><s:text name="role.model.message"/></p>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

	<h4>${model.application}</h4>

	<h5><s:text name="resources"/>:</h5>
    <ul>
      <s:iterator id="resource" value="model.resources">
      <li>
        <s:text name="role.model.id"/>: ${resource.id}<br/>
        <s:text name="role.model.name"/>: ${resource.name}<br/>
        <s:text name="role.model.permanent"/>: ${resource.permanent}<br/>
        <br/>
      </li>
      </s:iterator>
    </ul>

	<h5><s:text name="operations"/>:</h5>
    <ul>
      <s:iterator id="operation" value="model.operations">
        <li>
	        <s:text name="role.model.id"/>: ${operation.id}<br/>
        	<s:text name="role.model.name"/>: ${operation.name}<br/>
        	<s:text name="role.model.permanent"/>: ${operation.permanent}<br/>
        	<br/>
       	</li>
      </s:iterator>
    </ul>
    
    <h5><s:text name="roles"/>:</h5>
    <ul>
      <s:iterator id="role" value="model.roles">        
        <li>
            <s:text name="role.model.id"/>: ${role.id}<br/>
       		<s:text name="role.model.name"/>: ${role.name}<br/>
       		<s:text name="role.model.permanent"/>: ${role.permanent}<br/>
       		<s:text name="role.model.assignable"/>: ${role.assignable}<br/>
       		<s:text name="permissions"/>:
       		<ul>
       		  <s:iterator id="permission" value="#role.permissions">
       		  	<li>
       		  	  <s:text name="role.model.id"/>: ${permission.id}<br/>
       		  	  <s:text name="role.model.name"/>: ${permission.name}<br/>
       		  	  <s:text name="role.model.permanent"/>: ${permission.permanent}<br/>
       		  	  <s:text name="role.model.operation.id"/>: ${permission.operation}<br/>
       		  	  <s:text name="role.model.resource.id"/>: ${permission.resource}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul>
       		<s:text name="role.model.child.roles"/>:
       		<ul>
       		  <s:iterator id="childRole" value="#role.childRoles">
       		  	<li>
       		  	  <s:text name="role.model.role.id"/>: ${childRole}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul> 
       		<s:text name="role.model.parent.roles"/>:
       		<ul>
       		  <s:iterator id="parentRole" value="#role.parentRoles">
       		  	<li>
       		  	  <s:text name="role.model.role.id"/>: ${parentRole}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul> 
       	</li>
       	<br/>
      </s:iterator>
    </ul>
    
    <h5><s:text name="role.model.templates"/>:</h5>
    <ul>
        <s:iterator id="template" value="model.templates">        
        <li>
            <s:text name="role.model.id"/>: ${template.id}<br/>
       		<s:text name="role.model.name.prefix"/>: ${template.namePrefix}<br/>
       		<s:text name="role.model.permanent"/>: ${template.permanent}<br/>
       		<s:text name="role.model.assignable"/>: ${template.assignable}<br/>
       		<s:text name="role.model.delimeter"/>: ${template.delimiter}<br/>
       		<s:text name="permissions"/>:
       		<ul>
       		  <s:iterator id="permission" value="#template.permissions">
       		  	<li>
       		  	  <s:text name="role.model.id"/>: ${permission.id}<br/>
       		  	  <s:text name="role.model.name"/>: ${permission.name}<br/>
       		  	  <s:text name="role.model.permanent"/>: ${permission.permanent}<br/>
       		  	  <s:text name="role.model.operation.id"/>: ${permission.operation}<br/>
       		  	  <s:text name="role.model.resource.id"/>: ${permission.resource}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul>
       		<s:text name="role.model.child.roles"/>:
       		<ul>
       		  <s:iterator id="childRole" value="#template.childRoles">
       		  	<li>
       		  	  <s:text name="role.model.role.id"/>: ${childRole}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul> 
       		<s:text name="role.model.parent.roles"/>:
       		<ul>
       		  <s:iterator id="parentRole" value="#template.parentRoles">
       		  	<li>
       		  	  <s:text name="role.model.role.id"/>: ${parentRole}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul> 
       		<s:text name="role.model.child.templates"/>:
       		<ul>
       		  <s:iterator id="childTemplate" value="#template.childTemplates">
       		  	<li>
       		  	  <s:text name="role.model.template.id"/>: ${childTemplate}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul> 
       		<s:text name="role.model.parent.templates"/>:
       		<ul>
       		  <s:iterator id="parentTemplate" value="#template.parentTemplates">
       		  	<li>
       		  	  <s:text name="role.model.template.id"/>: ${parentTemplate}<br/>
       		  	</li>
       		  </s:iterator>
       		</ul> 
       	</li>
       	<br/>
      </s:iterator>
    </ul>

</body>
</s:i18n>
</html>
