#!/bin/sh

echo This script is very temporary. Please validate all input files in the source repository before blindly copying them in.
echo Ensure all artifacts have a valid POM.
echo This will be removed when the repository manager is in place.

echo

echo Options
echo  go - does the sync for real
echo  check - checks for changed files

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

for f in `find conf -maxdepth 1 -iname "*.sh"`
  do

  FROM=
  TO=
  NO_SSH=
  SSH_OPTS=
  # to prevent empty variable
  RSYNC_SSH="-z"

  source $f

  if [ -z $NO_SSH ]
  then
    RSYNC_SSH="--rsh=ssh $SSH_OPTS"
  fi

  # check for changed files
  if [ "$1" == "check" ]; then

    rsync -n --exclude=**/maven-metadata.xml* --exclude-from=$HOME/components/maven-meeper/src/bin/synchronize/syncopate/exclusions.txt --existing $RSYNC_OPTS -Lrtivzc "$RSYNC_SSH" $FROM $BASEDIR/$TO >> $CHANGED_LOG

  else

    echo "Syncing $FROM -> $TO"
    rsync --include=*/ --include=**/maven-metadata.xml* --exclude=* --exclude-from=$HOME/components/maven-meeper/src/bin/synchronize/syncopate/exclusions.txt $RSYNC_OPTS -Lrtivz "$RSYNC_SSH" $FROM $BASEDIR/$TO
    rsync --exclude-from=$HOME/components/maven-meeper/src/bin/synchronize/syncopate/exclusions.txt --ignore-existing $RSYNC_OPTS -Lrtivz "$RSYNC_SSH" $FROM $BASEDIR/$TO

  fi

done

if [ "$1" == "check" ]; then
  echo "*******************************************************************************"
  echo "*******************************  CHANGED FILES  *******************************"
  echo "*******************************************************************************"
  cat $CHANGED_LOG
  echo "*******************************************************************************"
  echo "*******************************************************************************"
  echo "*******************************************************************************"
fi
