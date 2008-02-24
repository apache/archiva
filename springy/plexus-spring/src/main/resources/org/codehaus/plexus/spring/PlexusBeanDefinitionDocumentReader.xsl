<?xml version="1.0" encoding="UTF-8"?>
<!--
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
  -->

<xsl:stylesheet
    version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:plexus="http://plexus.codehaus.org/spring">

<xsl:output method="xml"/>

<!-- 
  Convert a plexus descriptor to a spring XML context with help of the custom <plexus: namespace
  to handle IoC containers incompatibilities. 
 -->

<xsl:template match="/component-set">
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:plexus="http://plexus.codehaus.org/spring"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd
                           http://plexus.codehaus.org/spring http://plexus.codehaus.org/schemas/spring-1.0.xsd">
  <xsl:for-each select="components/component">

    <plexus:component>
      <xsl:attribute name="role">
        <xsl:value-of select="role" />
      </xsl:attribute>
      <xsl:if test="role-hint">
        <xsl:attribute name="role-hint">
          <xsl:value-of select="role-hint" />
        </xsl:attribute>
      </xsl:if>
      <xsl:attribute name="implementation">
        <xsl:value-of select="implementation" />
      </xsl:attribute>
      <xsl:if test="instanciation-strategy">
        <xsl:attribute name="instanciation-strategy">
          <xsl:value-of select="instanciation-strategy" />
        </xsl:attribute>
      </xsl:if>
      <xsl:for-each select="requirements/requirement">
        <plexus:requirement>
          <xsl:attribute name="name">
            <xsl:value-of select="field-name" />
          </xsl:attribute>
          <xsl:attribute name="role">
            <xsl:value-of select="role" />
          </xsl:attribute>
          <xsl:if test="role-hint">
            <xsl:attribute name="role-hint">
              <xsl:value-of select="role-hint" />
            </xsl:attribute>
          </xsl:if>
        </plexus:requirement>
      </xsl:for-each>
      <xsl:for-each select="configuration/*">
        <plexus:configuration>
          <xsl:attribute name="name">
            <xsl:value-of select="name(.)" />
          </xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="." />
          </xsl:attribute>
        </plexus:configuration>
      </xsl:for-each>
    </plexus:component>

  </xsl:for-each>
</beans>
</xsl:template>

</xsl:stylesheet>