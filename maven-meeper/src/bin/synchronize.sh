#!/bin/sh

# ------------------------------------------------------------------------
# 1. Sync Maven 1.x repositories to central
# 2. Convert Maven 1.x repository to Maven 2.x repository
# 3. Manual fixes
# 4. Sync Maven 2.x repositories to central
# 5. Sync the Maven 2.x repository to Ibiblio
# 6. Copy the mod_rewrite rules to the Maven 1.x repository @ Ibiblio
# ------------------------------------------------------------------------

dir=`pwd`
syncProperties=$dir/synchronize.properties

MODE=$1
PID=$$

(

RUNNING=`ps -ef | grep synchronize.sh | grep -v 'sh -c' | grep -v grep | grep -v $PID`
if [ ! -z "$RUNNING" ]; then
  echo Sync already running... exiting
  echo $RUNNING
  exit 1
fi

. $syncProperties

echo "Using the following settings:"
echo "CENTRAL_HOST = $CENTRAL_HOST"
echo "TOOLS_BASE = $TOOLS_BASE"
echo "SYNC_TOOLS = $SYNC_TOOLS"
echo "SYNCOPATE = $SYNCOPATE"
echo "REPOCLEAN = $REPOCLEAN"
echo "M1_M2_REWRITE_RULES = $M1_M2_REWRITE_RULES"

[ "$MODE" = "batch" ] && echo && echo "Press any key to continue, or hit ^C to quit." && echo

# ------------------------------------------------------------------------
# Syncopate: Sync the Maven 1.x repositories 
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to run syncopate, or hit ^C to quit." && echo

echo "Running Syncopate"

(
  cd $SYNCOPATE
  ./sync
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

# ------------------------------------------------------------------------
# Repoclean: converting the Maven 1.x repository to Maven 2.x 
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to run the m1 to m2 conversion, or hit ^C to quit." && echo

echo "Running Maven 1.x to Maven 2.x conversion ..."

(
  $REPOCLEAN/convert-m1-m2.sh $syncProperties
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

# ------------------------------------------------------------------------
# Manual fixes
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to run manual fixes, or hit ^C to quit." && echo

echo "Removing commons-logging 1.1-dev"

# hack prevent commons-logging-1.1-dev
CL=$MAVEN2_REPO/commons-logging/commons-logging
rm -rf $CL/1.1-dev
grep -v 1.1-dev $CL/maven-metadata.xml > $CL/maven-metadata.xml.tmp
mv $CL/maven-metadata.xml.tmp $CL/maven-metadata.xml
md5sum $CL/maven-metadata.xml > $CL/maven-metadata.xml.md5
sha1sum $CL/maven-metadata.xml > $CL/maven-metadata.xml.sha1

# ------------------------------------------------------------------------
# 4. Sync Maven 2.x repositories to central
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to sync Maven 2.x repositories to central, or hit ^C to quit." && echo

(
  cd $M2_SYNC
  ./m2-sync.sh go
)

# ------------------------------------------------------------------------
# Ibiblio synchronization: sync the central repository to Ibiblio 
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to run the sync to Ibiblio, or hit ^C to quit." && echo

echo "Synchronizing to ibiblio"

(
  cd $SYNC_TOOLS
  ./sync-central-to-ibiblio.sh $syncProperties
  retval=$?; if [ $retval != 0 ]; then exit $retval; fi
)
retval=$?; if [ $retval != 0 ]; then exit $retval; fi

# ------------------------------------------------------------------------
# Copy the mod_rewrite rules to the Maven 1.x repository
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to copy the m1 to m2 rewrite rules, or hit ^C to quit." && echo

echo "Copying rewrite rules into place"

cp $M1_M2_REWRITE_RULES $MAVEN1_REPO/.htaccess
    
scp $M1_M2_REWRITE_RULES maven@login.ibiblio.org:/public/html/maven/.htaccess   

) > $HOME/repository-staging/to-ibiblio/reports/sync/last-sync-results.txt 2>&1
