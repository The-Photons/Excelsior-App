/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package site.overwrite.encryptedfilesapp.cryptography;


import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NullCipher;
import javax.crypto.ShortBufferException;

/**
 * A CipherInputStream is composed of an InputStream and a Cipher so
 * that read() methods return data that are read in from the
 * underlying InputStream but have been additionally processed by the
 * Cipher.  The Cipher must be fully initialized before being used by
 * a CipherInputStream.
 *
 * <p> For example, if the Cipher is initialized for decryption, the
 * CipherInputStream will attempt to read in data and decrypt them,
 * before returning the decrypted data.
 *
 * <p> This class adheres strictly to the semantics, especially the
 * failure semantics, of its ancestor classes
 * java.io.FilterInputStream and java.io.InputStream.  This class has
 * exactly those methods specified in its ancestor classes, and
 * overrides them all.  Moreover, this class catches all exceptions
 * that are not thrown by its ancestor classes.  In particular, the
 * <code>skip</code> method skips, and the <code>available</code>
 * method counts only data that have been processed by the encapsulated Cipher.
 *
 * <p> It is crucial for a programmer using this class not to use
 * methods that are not defined or overriden in this class (such as a
 * new method or constructor that is later added to one of the super
 * classes), because the design and implementation of those methods
 * are unlikely to have considered security impact with regard to
 * CipherInputStream.
 *
 * @author  Li Gong, PhotonicGluon
 * @see     java.io.InputStream
 * @see     java.io.FilterInputStream
 * @see     javax.crypto.Cipher
 * @see     javax.crypto.CipherOutputStream
 *
 * @since 1.4
 */

public class MyCipherInputStream extends FilterInputStream {

    // the cipher engine to use to process stream data
    private Cipher cipher;

    // the underlying input stream
    private InputStream input;

    /**
     * Apparently the cipher input stream here has a limited buffer size of 512 bytes.
     * (See <a href="https://stackoverflow.com/a/49957428">https://stackoverflow.com/a/49957428</a>)
     * So we need to implemented this own class to remove this arbitrary limit.
     */
    private final byte[] ibuffer;

    // having reached the end of the underlying input stream
    private boolean done = false;

    /* the buffer holding data that have been processed by the cipher
       engine, but have not been read out */
    private byte[] obuffer;
    // the offset pointing to the next "new" byte
    private int ostart = 0;
    // the offset pointing to the last "new" byte
    private int ofinish = 0;
    // stream status
    private boolean closed = false;

    /**
     * private convenience function.
     *
     * Entry condition: ostart = ofinish
     *
     * Exit condition: ostart <= ofinish
     *
     * return (ofinish-ostart) (we have this many bytes for you)
     * return 0 (no data now, but could have more later)
     * return -1 (absolutely no more data)
     *
     * Note:  Exceptions are only thrown after the stream is completely read.
     * For AEAD ciphers a read() of any length will internally cause the
     * whole stream to be read fully and verify the authentication tag before
     * returning decrypted data or exceptions.
     */
    private int getMoreData() throws IOException {
        // Android-changed: The method was creating a new object every time update(byte[], int, int)
        // or doFinal() was called resulting in the old object being GCed. With do(byte[], int) and
        // update(byte[], int, int, byte[], int), we use already initialized obuffer.
        if (done) return -1;
        ofinish = 0;
        ostart = 0;
        int expectedOutputSize = cipher.getOutputSize(ibuffer.length);
        if (obuffer == null || expectedOutputSize > obuffer.length) {
            obuffer = new byte[expectedOutputSize];
        }
        int readin = input.read(ibuffer);
        if (readin == -1) {
            done = true;
            try {
                // doFinal resets the cipher and it is the final call that is made. If there isn't
                // any more byte available, it returns 0. In case of any exception is raised,
                // obuffer will get reset and therefore, it is equivalent to no bytes returned.
                ofinish = cipher.doFinal(obuffer, 0);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                obuffer = null;
                throw new IOException(e);
            } catch (ShortBufferException e) {
                obuffer = null;
                throw new IllegalStateException("ShortBufferException is not expected", e);
            }
        } else {
            // update returns number of bytes stored in obuffer.
            try {
                ofinish = cipher.update(ibuffer, 0, readin, obuffer, 0);
            } catch (IllegalStateException e) {
                obuffer = null;
                throw e;
            } catch (ShortBufferException e) {
                // Should not reset the value of ofinish as the cipher is still not invalidated.
                obuffer = null;
                throw new IllegalStateException("ShortBufferException is not expected", e);
            }
        }
        return ofinish;
    }

    /**
     * Constructs a CipherInputStream from an InputStream and a
     * Cipher.
     * <br>Note: if the specified input stream or cipher is
     * null, a NullPointerException may be thrown later when
     * they are used.
     * @param is the to-be-processed input stream
     * @param c an initialized Cipher object
     */
    public MyCipherInputStream(InputStream is, Cipher c, int bufferSize) {
        super(is);
        input = is;
        cipher = c;
        ibuffer = new byte[bufferSize];
    }

    /**
     * Constructs a CipherInputStream from an InputStream without
     * specifying a Cipher. This has the effect of constructing a
     * CipherInputStream using a NullCipher.
     * <br>Note: if the specified input stream is null, a
     * NullPointerException may be thrown later when it is used.
     * @param is the to-be-processed input stream
     */
    protected MyCipherInputStream(InputStream is) {
        super(is);
        input = is;
        cipher = new NullCipher();
        ibuffer = new byte[512];
    }

    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned. This method blocks until input data
     * is available, the end of the stream is detected, or an exception
     * is thrown.
     * <p>
     *
     * @return  the next byte of data, or <code>-1</code> if the end of the
     *          stream is reached.
     * @exception  IOException  if an I/O error occurs.
     * @since JCE1.2
     */
    public int read() throws IOException {
        if (ostart >= ofinish) {
            // we loop for new data as the spec says we are blocking
            int i = 0;
            while (i == 0) i = getMoreData();
            if (i == -1) return -1;
        }
        return ((int) obuffer[ostart++] & 0xff);
    };

    /**
     * Reads up to <code>b.length</code> bytes of data from this input
     * stream into an array of bytes.
     * <p>
     * The <code>read</code> method of <code>InputStream</code> calls
     * the <code>read</code> method of three arguments with the arguments
     * <code>b</code>, <code>0</code>, and <code>b.length</code>.
     *
     * @param      b   the buffer into which the data is read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> is there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.InputStream#read(byte[], int, int)
     * @since      JCE1.2
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes. This method blocks until some input is
     * available. If the first argument is <code>null,</code> up to
     * <code>len</code> bytes are read and discarded.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in the destination array
     *                   <code>buf</code>
     * @param      len   the maximum number of bytes read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.InputStream#read()
     * @since      JCE1.2
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (ostart >= ofinish) {
            // we loop for new data as the spec says we are blocking
            int i = 0;
            while (i == 0) i = getMoreData();
            if (i == -1) return -1;
        }
        if (len <= 0) {
            return 0;
        }
        int available = ofinish - ostart;
        if (len < available) available = len;
        if (b != null) {
            System.arraycopy(obuffer, ostart, b, off, available);
        }
        ostart = ostart + available;
        return available;
    }

    /**
     * Skips <code>n</code> bytes of input from the bytes that can be read
     * from this input stream without blocking.
     *
     * <p>Fewer bytes than requested might be skipped.
     * The actual number of bytes skipped is equal to <code>n</code> or
     * the result of a call to
     * {@link #available() available},
     * whichever is smaller.
     * If <code>n</code> is less than zero, no bytes are skipped.
     *
     * <p>The actual number of bytes skipped is returned.
     *
     * @param      n the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     * @exception  IOException  if an I/O error occurs.
     * @since JCE1.2
     */
    public long skip(long n) throws IOException {
        int available = ofinish - ostart;
        if (n > available) {
            n = available;
        }
        if (n < 0) {
            return 0;
        }
        ostart += n;
        return n;
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking. The <code>available</code> method of
     * <code>InputStream</code> returns <code>0</code>. This method
     * <B>should</B> be overridden by subclasses.
     *
     * @return     the number of bytes that can be read from this input stream
     *             without blocking.
     * @exception  IOException  if an I/O error occurs.
     * @since      JCE1.2
     */
    public int available() throws IOException {
        return (ofinish - ostart);
    }

    /**
     * Closes this input stream and releases any system resources
     * associated with the stream.
     * <p>
     * The <code>close</code> method of <code>CipherInputStream</code>
     * calls the <code>close</code> method of its underlying input
     * stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @since JCE1.2
     */
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        input.close();

        // Android-removed: Removed a now-inaccurate comment
        if (!done) {
            try {
                cipher.doFinal();
            }
            catch (BadPaddingException | IllegalBlockSizeException ex) {
                // Android-changed: Added throw if bad tag is seen.  See b/31590622.
                if (ex instanceof AEADBadTagException) {
                    throw new IOException(ex);
                }
            }
        }
        ostart = 0;
        ofinish = 0;
    }

    /**
     * Tests if this input stream supports the <code>mark</code>
     * and <code>reset</code> methods, which it does not.
     *
     * @return  <code>false</code>, since this class does not support the
     *          <code>mark</code> and <code>reset</code> methods.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     * @since   JCE1.2
     */
    public boolean markSupported() {
        return false;
    }
}
