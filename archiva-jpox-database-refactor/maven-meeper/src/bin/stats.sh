#!/bin/sh

JARS=`find $M2_REPO -iname "*.jar" | wc -l`
POMS=`find $M2_REPO -iname "*.pom" | wc -l`
BINJARS=`find $M2_REPO -iname "*.jar" ! -iname "*-sources.jar" ! -iname "*-javadoc.jar" | wc -l`
SOURCES=`find $M2_REPO -iname "*-sources.jar" | wc -l`
JAVADOCS=`find $M2_REPO -iname "*-javadoc.jar" | wc -l`

echo total jars:  $JARS
echo poms:        $POMS
echo binary jars: $BINJARS
echo sources:     $SOURCES
echo javadocs:    $JAVADOCS
