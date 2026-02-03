/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over.helper;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.IArgumentMatcher;
import org.easymock.EasyMock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class InputStreamMatcher implements IArgumentMatcher {

    private static final Log LOG = LogFactory.getLog(InputStreamMatcher.class);

    public static InputStream streamContains(final byte[] expectedContents) {
        EasyMock.reportMatcher(new InputStreamMatcher(expectedContents));
        return null;
    }

    private final byte[] expectedContents;

    public InputStreamMatcher(final byte[] expectedContents) {
        this.expectedContents = Arrays.copyOf(expectedContents, expectedContents.length);
    }

    public boolean matches(final Object argument) {
        if (!(argument instanceof InputStream))
            return false;

        try {
            final byte[] actual = IOUtils.toByteArray((InputStream) argument);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Expected (md5) = " + DigestUtils.md5Hex(expectedContents));
                LOG.debug("Actual   (md5) = " + DigestUtils.md5Hex(actual));
            }
            return Arrays.equals(expectedContents, actual);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to read contents of InputStream", ioe);
        }
    }

    public void appendTo(final StringBuffer stringBuffer) {
        stringBuffer.append("Expected InputStream with contents (md5) = ")
                .append(DigestUtils.md5Hex(expectedContents));
    }
}
