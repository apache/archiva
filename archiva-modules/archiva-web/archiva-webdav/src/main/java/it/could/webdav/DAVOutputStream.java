/* ========================================================================== *
 *         Copyright (C) 2004-2006, Pier Fumagalli <http://could.it/>         *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
package it.could.webdav;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * <p>A specialized {@link OutputStream} to write to {@link DAVResource}s.</p>
 * 
 * <p>When writing to this {@link OutputStream} the data will be written to
 * a temporary file. This temporary file will be moved to its final destination
 * (the original file identifying the resource) when the {@link #close()}
 * method is called.</p>
 *
 * <p>This specialized {@link OutputStream} never throws {@link IOException}s,
 * but rather relies on the unchecked {@link DAVException} to notify the
 * framework of the correct DAV errors.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVOutputStream extends OutputStream {

    /** <p>The original resource {@link File}.</p> */
    private File temporary = null;
    /** <p>The {@link OutputStream} of the temporary {@link File}. </p> */
    protected OutputStream output = null;
    /** <p>The {@link DAVResource} associated with this instance. </p> */
    private DAVResource resource = null;

    /**
     * <p>Create a new {@link DAVOutputStream} instance.</p>
     */
    protected DAVOutputStream(DAVResource resource) {
        if (resource == null) throw new NullPointerException();
        this.resource = resource;
        init(resource);
    }
    
    protected void init(DAVResource resource) {
        try {
            this.temporary = resource.getParent().getFile();
            this.temporary = File.createTempFile(DAVResource.PREFIX,
                                                 DAVResource.SUFFIX,
                                                 this.temporary);
            this.output = new FileOutputStream(this.temporary);
        } catch (IOException e) {
            String message = "Unable to create temporary file";
            throw new DAVException(507, message, e, resource);
        }
    }

    /**
     * <p>Rename the temporary {@link File} to the original one.</p>
     */
    protected void rename(File temporary, File original)
    throws IOException {
        if ((original.exists()) && (!original.delete())) {
            throw new IOException("Unable to delete original file");
        }
        if (!temporary.renameTo(original)) {
            throw new IOException("Unable to rename temporary file");
        }
    }

    /**
     * <p>Abort any data written to the temporary file and delete it.</p>
     */
    public void abort() {
        if (this.temporary.exists()) this.temporary.delete();
        if (this.output != null) try {
            this.output.close();
        } catch (IOException exception) {
            // Swallow the IOException on close
        } finally {
            this.output = null;
        }
    }

    /**
     * <p>Close this {@link OutputStream} {@link #rename(File,File) renaming}
     * the temporary file to the {@link DAVResource#getFile() original} one.</p>
     */
    public void close() {
        if (this.output == null) return;
        try {
            /* What kind of event should this invocation trigger? */
            int event = this.resource.getFile().exists() ?
                        DAVListener.RESOURCE_MODIFIED:
                        DAVListener.RESOURCE_CREATED;

            /* Make sure that everything is closed and named properly */
            this.output.close();
            this.output = null;
            this.rename(this.temporary, this.resource.getFile());

            /* Send notifications to all listeners of the repository */
            this.resource.getRepository().notify(this.resource, event);

        } catch (IOException e) {
            String message = "Error processing temporary file";
            throw new DAVException(507, message, e, this.resource);
        } finally {
            this.abort();
        }
    }

    /**
     * <p>Flush any unwritten data to the disk.</p>
     */
    public void flush() {
        if (this.output == null) throw new IllegalStateException("Closed");
        try {
            this.output.flush();
        } catch (IOException e) {
            this.abort();
            String message = "Unable to flush buffers";
            throw new DAVException(507, message, e, this.resource);
        }
    }

    /**
     * <p>Write data to this {@link OutputStream}.</p>
     */
    public void write(int b) {
        if (this.output == null) throw new IllegalStateException("Closed");
        try {
            this.output.write(b);
        } catch (IOException e) {
            this.abort();
            String message = "Unable to write data";
            throw new DAVException(507, message, e, this.resource);
        }
    }
    
    /**
     * <p>Write data to this {@link OutputStream}.</p>
     */
    public void write(byte b[]) {
        if (this.output == null) throw new IllegalStateException("Closed");
        try {
            this.output.write(b);
        } catch (IOException e) {
            this.abort();
            String message = "Unable to write data";
            throw new DAVException(507, message, e, this.resource);
        }
    }
    
    /**
     * <p>Write data to this {@link OutputStream}.</p>
     */
    public void write(byte b[], int o, int l) {
        if (this.output == null) throw new IllegalStateException("Closed");
        try {
            this.output.write(b, o, l);
        } catch (IOException e) {
            this.abort();
            String message = "Unable to write data";
            throw new DAVException(507, message, e, this.resource);
        }
    }
    
    /**
     * <p>Finalize this {@link DAVOutputStream} instance.</p>
     */
    public void finalize() {
        this.abort();
    }
}
