#!/bin/sh

# ------------------------------------------------------------------------
# 1. Sync Maven 1.x repositories
# 2. Convert Maven 1.x repository to Maven 2.x repository
# 3. Manual fixes
# 4. Sync the Maven 2.x repository to Ibiblio
# ------------------------------------------------------------------------

PID=$$
RUNNING=`ps -ef | grep synchronize.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  echo Sync already running... exiting
  echo $RUNNING
  exit 1
fi

. synchronize.properties

# ------------------------------------------------------------------------
# Syncopate: the Maven 1.x repository 
# ------------------------------------------------------------------------
echo Running Syncopate

(
  cd $SYNCOPATE
  ./sync
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

# ------------------------------------------------------------------------
# Repoclean: converting the Maven 1.x repository to Maven 2.x 
# ------------------------------------------------------------------------
echo Running repoclean

(
  $REPOCLEAN/sync-repoclean.sh
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

# ------------------------------------------------------------------------
# Manual fixes
# ------------------------------------------------------------------------
echo Removing commons-logging 1.1-dev

# hack prevent commons-logging-1.1-dev
CL=$HOME/repository-staging/to-ibiblio/maven2/commons-logging/commons-logging
rm -rf $CL/1.1-dev
grep -v 1.1-dev $CL/maven-metadata.xml > $CL/maven-metadata.xml.tmp
mv $CL/maven-metadata.xml.tmp $CL/maven-metadata.xml
md5sum $CL/maven-metadata.xml > $CL/maven-metadata.xml.md5
sha1sum $CL/maven-metadata.xml > $CL/maven-metadata.xml.sha1

# ------------------------------------------------------------------------
# Ibiblio synchronization: sync the central repository to Ibiblio 
# ------------------------------------------------------------------------
echo Synchronizing to ibiblio

(
  cd $SYNC_TOOLS
  ./sync-central-to-ibiblio.sh
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

