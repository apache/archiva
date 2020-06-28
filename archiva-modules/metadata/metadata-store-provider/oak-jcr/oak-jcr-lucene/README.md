oak-jcr-lucene
==============


This module is only to provide the oak-lucene dependency with lucene shaded into a different
java package. 
Jackrabbit Oak has dependencies to Apache Lucene 4, which is very old and merely out of support.

We move the lucene dependencies to the package shaded_oak.org.apache.lucene to allow using
a more recent version for Archiva.

For some reason the oak-lucene (1.22.3) package is a fat jar that contains the lucene classes itself,
therefore we are excluding the lucene dependencies in the pom.

