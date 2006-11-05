#!/bin/sh

POM=bundle.tmp/project.xml

maven2=`cat ${POM} | grep '<modelVersion>' ` 

echo ${maven2}
if [ ! -z "${maven2}" ]
then
  echo " ========= MAVEN 2 ========= "
fi

POM=bundle.tmp/pom.xml

maven2=`cat ${POM} | grep '<modelVersion>' `

echo ${maven2}
if [ ! -z "${maven2}" ]
then
  echo " ========= MAVEN 2 ========= "
fi

