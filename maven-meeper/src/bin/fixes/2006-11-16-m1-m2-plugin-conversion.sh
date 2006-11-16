#!/bin/sh

# Thu Nov 16 06:55:11 EST 2006
# A little script which gets rid of *.plugin files that were created by Archiva by mistake. We
# don't want to convert Maven 1.x plugins to Maven 2.x plugins as they won't even run. Bad Jelly, bad.

find . -name '*.plugin' -exec rm -f {} \;
