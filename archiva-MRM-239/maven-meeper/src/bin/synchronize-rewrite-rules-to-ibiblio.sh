#!/bin/bash

[ "$1" = "" ] && echo && echo "You must pass in the synchronize.properties file!" && echo && exit

. $1

# ------------------------------------------------------------------------
# Copy the mod_rewrite rules to the Maven 1.x repository
# ------------------------------------------------------------------------

[ "$MODE" = "batch" ] && echo && echo "Press any key to copy the m1 to m2 rewrite rules, or hit ^C to quit." && echo

echo "Copying rewrite rules into place"

cp $M1_M2_REWRITE_RULES $MAVEN1_REPO/.htaccess

scp $M1_M2_REWRITE_RULES $IBIBLIO_SYNC_HOST:$M1_IBIBLIO_SYNC_DIR/.htaccess

