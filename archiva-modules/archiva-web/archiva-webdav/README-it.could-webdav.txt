This library contains the patched sources to the it.could simple WebDAV library r280, licensed under the Apache License 2.0.

http://could.it/main/a-simple-approach-to-webdav.html

To later return to a released version (after the patches have been incorporated and released):
- remove src/main/java/it and src/main/java/org/betaversion
- remove <build> <resources> from the POM
- replace the servlet-api dependency in the POM with it.could webdav.
