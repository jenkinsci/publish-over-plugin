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
import static org.junit.Assert.assertEquals;

@SuppressWarnings("PMD.MagicNumberCheck")
public class JellySupportTest {

    @Test public void textAreaHeightMinimumIfNoContent() {
        assertEquals(3, JellySupport.textAreaHeight(3, null));
    }

    @Test public void textAreaHeightMinimumIfEmpty() {
        assertEquals(1, JellySupport.textAreaHeight(1, ""));
    }

    @Test public void textAreaHeightUnixContentLines() {
        assertEquals(3, JellySupport.textAreaHeight(1, "one\ntwo\nthree"));
    }

    @Test public void textAreaHeightWindowsContentLines() {
        assertEquals(3, JellySupport.textAreaHeight(1, "one\r\ntwo\r\nthree"));
    }

    @Test public void textAreaHeightMinimumIfGreaterThanHeight() {
        assertEquals(3, JellySupport.textAreaHeight(3, "one\ntwo"));
    }

    @Test public void textAreaHeightContentHeightIfGreaterThanMinimum() {
        assertEquals(3, JellySupport.textAreaHeight(2, "one\ntwo\nthree"));
    }

    @Test public void textAreaHeightEnforceMinimumMinimum() {
        assertEquals(JellySupport.MINIMUM_MINIMUM_HEIGHT, JellySupport.textAreaHeight(JellySupport.MINIMUM_MINIMUM_HEIGHT - 1, null));
    }

}
