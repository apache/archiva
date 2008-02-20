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
    xmlns:plexus="org.apache.maven.archiva.common.spring.PlexusToSpringUtils">
<!--
    FIXME replace xalan extension mecanism to call static methods with XPathFunctions
    @see http://www.ibm.com/developerworks/library/x-xalanextensions.html
 -->

<xsl:output method="xml" indent="yes"
    doctype-public="-//SPRING//DTD BEAN 2.0//EN"
    doctype-system="http://www.springframework.org/dtd/spring-beans-2.0.dtd" />

<xsl:template match="/component-set">
<beans>
  <xsl:for-each select="components/component">
    <bean>
      <xsl:choose>
        <xsl:when test="role-hint">
          <xsl:attribute name="id">
            <xsl:value-of select="concat( role, '#', role-hint )" />
          </xsl:attribute>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="id">
            <xsl:value-of select="role" />
          </xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:attribute name="class">
        <xsl:value-of select="implementation" />
      </xsl:attribute>
      <xsl:if test="instanciation-strategy/text() = 'per-lookup'">
        <xsl:attribute name="scope">prototype</xsl:attribute>
      </xsl:if>
      <xsl:if test="plexus:isInitializable( implementation/text() )">
        <xsl:attribute name="init-method">initialize</xsl:attribute>
      </xsl:if>
      <xsl:if test="plexus:isDisposable( implementation/text() )">
        <xsl:attribute name="init-method">dispose</xsl:attribute>
      </xsl:if>
      <xsl:for-each select="requirements/requirement">
        <property>
          <xsl:attribute name="name">
            <xsl:value-of select="field-name" />
          </xsl:attribute>
          <xsl:choose>
            <xsl:when test="role-hint">
              <xsl:attribute name="ref">
                <xsl:value-of select="concat( role, '#', role-hint )" />
              </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="ref">
                <xsl:value-of select="role" />
              </xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
        </property>
      </xsl:for-each>
      <xsl:for-each select="configuration/*">
        <property>
          <xsl:attribute name="name">
            <xsl:value-of select="plexus:toCamelCase( name(.) )" />
          </xsl:attribute>
          <xsl:attribute name="value">
            <xsl:value-of select="." />
          </xsl:attribute>
        </property>
      </xsl:for-each>
    </bean>
  </xsl:for-each>
</beans>
</xsl:template>

</xsl:stylesheet>