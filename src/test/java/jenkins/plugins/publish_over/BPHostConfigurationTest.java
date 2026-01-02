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

import org.junit.jupiter.api.Test;

import java.io.Serial;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("PMD.SignatureDeclareThrowsException") class BPHostConfigurationTest {

    private final BPHostConfiguration hostConfig = new ConcreteBPHostConfiguration();

    @Test
    void testIsAbsoluteFalseForNull() {
        assertFalse(hostConfig.isDirectoryAbsolute(null));
    }

    @Test
    void testIsAbsoluteFalseForEmpty() {
        assertFalse(hostConfig.isDirectoryAbsolute(""));
    }

    @Test
    void testIsAbsoluteFalseForRelativeWin() {
        assertFalse(hostConfig.isDirectoryAbsolute("some\\file\\path"));
    }

    @Test
    void testIsAbsoluteFalseForRelativeUnix() {
        assertFalse(hostConfig.isDirectoryAbsolute("some/file/path"));
    }

    @Test
    void testIsAbsoluteTrueForWin() {
        assertTrue(hostConfig.isDirectoryAbsolute("\\some\\file\\path"));
    }

    @Test
    void testIsAbsoluteTrueForUnix() {
        assertTrue(hostConfig.isDirectoryAbsolute("/some/file/path"));
    }

    @Test
    void testIsAbsoluteTrueForWinRoot() {
        assertTrue(hostConfig.isDirectoryAbsolute("\\"));
    }

    @Test
    void testIsAbsoluteTrueForUnixRoot() {
        assertTrue(hostConfig.isDirectoryAbsolute("/"));
    }

    private static class ConcreteBPHostConfiguration extends BPHostConfiguration {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public BPClient createClient(final BPBuildInfo buildInfo) {
            return null;
        }

        @Serial
        @Override
        public Object readResolve() {
            return super.readResolve();
        }

    }

}
