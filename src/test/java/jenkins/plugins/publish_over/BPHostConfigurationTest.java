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

package jenkins.plugins.publish_over;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class BPHostConfigurationTest {

    private final BPHostConfiguration hostConfig = new ConcreteBPHostConfiguration();

    @Test public void testIsAbsoluteFalseForNull() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute(null));
    }

    @Test public void testIsAbsoluteFalseForEmpty() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute(""));
    }

    @Test public void testIsAbsoluteFalseForRelativeWin() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute("some\\file\\path"));
    }

    @Test public void testIsAbsoluteFalseForRelativeUnix() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute("some/file/path"));
    }

    @Test public void testIsAbsoluteTrueForWin() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("\\some\\file\\path"));
    }

    @Test public void testIsAbsoluteTrueForUnix() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("/some/file/path"));
    }

    @Test public void testIsAbsoluteTrueForWinRoot() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("\\"));
    }

    @Test public void testIsAbsoluteTrueForUnixRoot() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("/"));
    }

    private static class ConcreteBPHostConfiguration extends BPHostConfiguration {

        private static final long serialVersionUID = 1L;

        @Override
        public BPClient createClient(final BPBuildInfo buildInfo) {
            return null;
        }

        @Override
        public Object readResolve() {
            return super.readResolve();
        }

    }

}
