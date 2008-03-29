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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * <p>A specialized {@link InputStream} to read from {@link DAVResource}s.</p>
 * 
 * <p>This specialized {@link InputStream} never throws {@link IOException}s,
 * but rather relies on the unchecked {@link DAVException} to notify the
 * framework of the correct DAV errors.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public class DAVInputStream extends InputStream {

    /** <p>The {@link InputStream} of the source {@link File}. </p> */
    protected InputStream input = null;
    /** <p>The {@link DAVResource} associated with this instance. </p> */
    private DAVResource resource = null;

    /**
     * <p>Create a new {@link DAVInputStream} instance.</p>
     */
    protected DAVInputStream(DAVResource resource) {
        if (resource == null) throw new NullPointerException();
        init(resource);
    }
    
    protected void init(DAVResource resource)
    {
        try {
            this.input = new FileInputStream(resource.getFile());
        } catch (IOException e) {
            String message = "Unable to read from resource";
            throw new DAVException (403, message, e, resource);
        }
    }

    /**
     * <p>Read data from this {@link InputStream}.</p>
     */
    public int read() {
        if (this.input == null) throw new IllegalStateException("Closed");
        try {
            return input.read();
        } catch (IOException e) {
            throw new DAVException(403, "Can't read data", e, this.resource);
        }
    }

    /**
     * <p>Read data from this {@link InputStream}.</p>
     */
    public int read(byte b[]) {
        if (this.input == null) throw new IllegalStateException("Closed");
        try {
            return input.read(b);
        } catch (IOException e) {
            throw new DAVException(403, "Can't read data", e, this.resource);
        }
    }

    /**
     * <p>Read data from this {@link InputStream}.</p>
     */
    public int read(byte b[], int off, int len) {
        if (this.input == null) throw new IllegalStateException("Closed");
        try {
            return input.read(b, off, len);
        } catch (IOException e) {
            throw new DAVException(403, "Can't read data", e, this.resource);
        }
    }

    /**
     * <p>Skip a specified amount of data reading from this
     * {@link InputStream}.</p>
     */
    public long skip(long n) {
        if (this.input == null) throw new IllegalStateException("Closed");
        try {
            return input.skip(n);
        } catch (IOException e) {
            throw new DAVException(403, "Can't skip over", e, this.resource);
        }
    }

    /**
     * <p>Return the number of bytes that can be read or skipped from this
     * {@link InputStream} without blocking.</p>
     */
    public int available() {
        if (this.input == null) throw new IllegalStateException("Closed");
        try {
            return input.available();
        } catch (IOException e) {
            throw new DAVException(403, "Can't skip over", e, this.resource);
        }
    }

    /**
     * <p>Return the number of bytes that can be read or skipped from this
     * {@link InputStream} without blocking.</p>
     */
    public void close() {
        if (this.input == null) return;
        try {
            this.input.close();
        } catch (IOException e) {
            throw new DAVException(403, "Can't close", e, this.resource);
        } finally {
            this.input = null;
        }
    }
    
    /**
     * <p>Marks the current position in this {@link InputStream}.</p>
     */
    public void mark(int readlimit) {
        if (this.input == null) throw new IllegalStateException("Closed");
        this.input.mark(readlimit);
    }
    
    /**
     * <p>Repositions this stream to the position at the time the
     * {@link #mark(int)} method was last called on this
     * {@link InputStream}.</p>
     */
    public void reset() {
        if (this.input == null) throw new IllegalStateException("Closed");
        try {
            input.reset();
        } catch (IOException e) {
            throw new DAVException(403, "Can't reset", e, this.resource);
        }
    }
    
    /**
     * <p>Tests if this {@link InputStream} supports the {@link #mark(int)}
     * and {@link #reset()} methods.</p>
     */
    public boolean markSupported() {
        if (this.input == null) throw new IllegalStateException("Closed");
        return this.input.markSupported();
    }
}
