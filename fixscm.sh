#!/bin/bash

# How to execute
#   export PSEC_HOME=`pwd`
#   find . -name "pom.xml" -print -exec $PSEC_HOME/fixscm.sh {} \;

POM=$1

if [ ! -f $POM ] ; then
    echo "ERROR: Unable to find pom - $POM"
    exit 1
fi

BASEDIR=`dirname $POM`
NOW=`date +%Y%m%d.%H%M%S`

CURDIR=`pwd`
cd `dirname $0`
SCRIPTHOME=`pwd`
cd $BASEDIR

SVNURL=`svn info | grep URL | sed -e "s@URL: http[s]://@@"`
FISHEYEURL=`echo $SVNURL | sed -e "s@svn.codehaus.org@fisheye.codehaus.org/browse@g"`

BACKUP=pom.xml-${NOW}~

POMFILE=`basename $POM`

cp $POMFILE $BACKUP

SCRIPTHOME=`dirname $SCRIPTHOME`
POMREL=`pwd | sed -e "s@$SCRIPTHOME@@"`

echo "Fixing <scm> in $POMREL/pom.xml"

cat $BACKUP | sed -e "s@scm:svn:\(http[s]*\)[^< ]*@scm:svn:\1://$SVNURL@g" | sed "s@http://fisheye[^<]*@http://$FISHEYEURL@g" > pom.xml

cd $CURDIR

