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

<div class="rbac-navigation-bar">

<s:url id="rolesUrl" action="roles" namespace="/security" method="list"/>
<s:url id="permissionsUrl" action="permissions" namespace="/security" method="list"/>
<s:url id="operationsUrl" action="operations" namespace="/security" method="list"/>
<s:url id="resourcesUrl" action="resources" namespace="/security" method="list"/>

<s:a href="%{rolesUrl}"><s:text name="roles"/></s:a> | 
<s:a href="%{permissionsUrl}"><s:text name="permissions"/></s:a> | 
<s:a href="%{operationsUrl}"><s:text name="operations"/></s:a> | 
<s:a href="%{resourcesUrl}"><s:text name="resources"/></s:a> 

</div>
