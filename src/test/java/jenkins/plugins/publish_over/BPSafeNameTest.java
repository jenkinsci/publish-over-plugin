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

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static hudson.util.FormValidation.Kind.*;

public class BPSafeNameTest {

    @Test public void testSingleCharacterOk() {
        assertOk("a");
    }

    @Test public void testWithWhitespaceOk() {
        assertOk(" hello world ");
    }

    @Test public void testNullNotOk() {
        assertNok(null);
    }

    @Test public void testAllWhitespaceNotOk() {
        assertNok("  ");
    }

    @Test public void testLessThanNotOk() {
        assertNok("<");
    }

    @Test public void testWithLessThanNotOk() {
        assertNok("myname<isnotok");
    }

    @Test public void testWithAmpNotOk() {
        assertNok("myname&isnotok");
    }

    @Test public void testWithSingleQuoteNotOk() {
        assertNok("myname'isnotok");
    }

    @Test public void testWithDoubleQuoteNotOk() {
        assertNok("myname\"isnotok");
    }

    @Test public void testWithBackSlashNotOk() {
        assertNok("myname\\isnotok");
    }

    private void assertOk(final String name) {
        assertEquals(OK, BPSafeName.validateName(name).kind);
    }

    private void assertNok(final String name) {
        assertEquals(ERROR, BPSafeName.validateName(name).kind);
    }

}
