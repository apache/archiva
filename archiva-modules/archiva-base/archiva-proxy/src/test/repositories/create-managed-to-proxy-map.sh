#!/bin/bash


MYWD=`pwd`

function makeListing()
{
    LISTID=$1

    cd $MYWD/$LISTID
    find . -type f -not -wholename "*/\.*" | sort > $MYWD/$LISTID.tmp
}

function isInRepo()
{
    LISTID=$1
    FILEID=$2

    grep -q "$FILEID" $MYWD/$LISTID.tmp
    RETCODE=$?
    if [ $RETCODE -eq 0 ] ; then
        LISTID=${LISTID/proxied/}
        echo "[${LISTID:0:1}]"
    else
        echo "   "
    fi
}

makeListing "managed"
makeListing "proxied1"
makeListing "proxied2"

cd $MYWD

TS=`date`

echo "$0 - executed on $TS"
echo ""
echo "Determining location of files."
echo " Key: [m] == managed"
echo "      [1] == proxy 1 (proxied1)"
echo "      [2] == proxy 2 (proxied2)"
echo ""
echo " -m- -1- -2- | -------------------------------------------- "

FILELIST=`cat managed.tmp proxied1.tmp proxied2.tmp | sort -u`

for FF in $FILELIST
do
    INMANAGED=`isInRepo "managed" "$FF"`
    INPROXY1=`isInRepo "proxied1" "$FF"`
    INPROXY2=`isInRepo "proxied2" "$FF"`

    echo " $INMANAGED $INPROXY1 $INPROXY2 | $FF"
done

echo " --- --- --- | -------------------------------------------- "

rm -f managed.tmp proxied1.tmp proxied2.tmp

