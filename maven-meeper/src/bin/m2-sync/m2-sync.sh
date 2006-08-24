#!/bin/sh

echo This script is very temporary. Please validate all input files in the source repository before blindly copying them in.
echo Ensure all artifacts have a valid POM.
echo This will be removed when the repository manager is in place.

echo

echo
echo For a better explanation of the output flags please check --itemize-changes at rsync man page
echo

if [ "$1" == "go" ]; then
  echo Doing sync for real
else
  echo Not syncing
  RSYNC_OPTS="$RSYNC_OPTS -n"
fi

BASEDIR=$HOME/repository-staging/to-ibiblio/maven2
CHANGED_LOG=/tmp/sync-changed.log
rm $CHANGED_LOG

for f in `find conf -iname "*.sh"`
  do

  FROM=
  TO=
  NO_SSH=
  SSH_OPTS=
  RSYNC=

  source $f

  if [ -z $NO_SSH ]
  then
    RSYNC="rsync --rsh=ssh $SSH_OPTS"
  else
    RSYNC="rsync"
  fi

  echo "Syncing $FROM -> $TO"
  "$RSYNC" --include=*/ --include=**/maven-metadata.xml* --exclude=* --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt $RSYNC_OPTS -Lrtivz $FROM $BASEDIR/$TO
  "$RSYNC" --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt --ignore-existing $RSYNC_OPTS -Lrtivz $FROM $BASEDIR/$TO

  # check for changed files
  "$RSYNC" -n --exclude=**/maven-metadata.xml* --exclude-from=$HOME/components/maven-meeper/src/bin/syncopate/exclusions.txt --existing $RSYNC_OPTS -Lrtivzc $FROM $BASEDIR/$TO >> $CHANGED_LOG

done

echo "*******************************************************************************"
echo "*******************************  CHANGED FILES  *******************************"
echo "*******************************************************************************"
cat $CHANGED_LOG
echo "*******************************************************************************"
echo "*******************************************************************************"
echo "*******************************************************************************"

