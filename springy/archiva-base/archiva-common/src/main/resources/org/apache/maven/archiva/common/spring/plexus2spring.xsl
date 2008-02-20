<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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
            <xsl:value-of select="name(.)" />
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