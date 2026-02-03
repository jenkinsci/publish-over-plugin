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

import static org.junit.jupiter.api.Assertions.assertEquals;

class JellySupportTest {

    @Test
    void textAreaHeightMinimumIfNoContent() {
        final int minimumHeight = 3;
        assertEquals(minimumHeight, JellySupport.textAreaHeight(minimumHeight, null));
    }

    @Test
    void textAreaHeightMinimumIfEmpty() {
        final int minimumHeight = 1;
        assertEquals(minimumHeight, JellySupport.textAreaHeight(minimumHeight, ""));
    }

    @Test
    void textAreaHeightUnixContentLines() {
        final int expectedHeight = 3;
        final int requestedMinimum = 1;
        assertEquals(expectedHeight, JellySupport.textAreaHeight(requestedMinimum, "one\ntwo\nthree"));
    }

    @Test
    void textAreaHeightWindowsContentLines() {
        final int expectedHeight = 3;
        final int requestedMinimum = 1;
        assertEquals(expectedHeight, JellySupport.textAreaHeight(requestedMinimum, "one\r\ntwo\r\nthree"));
    }

    @Test
    void textAreaHeightMinimumIfGreaterThanHeight() {
        final int minimumHeight = 3;
        assertEquals(minimumHeight, JellySupport.textAreaHeight(minimumHeight, "one\ntwo"));
    }

    @Test
    void textAreaHeightContentHeightIfGreaterThanMinimum() {
        final int expectedHeight = 3;
        final int requestedMinimum = 2;
        assertEquals(expectedHeight, JellySupport.textAreaHeight(requestedMinimum, "one\ntwo\nthree"));
    }

    @Test
    void textAreaHeightEnforceMinimumMinimum() {
        assertEquals(JellySupport.MINIMUM_MINIMUM_HEIGHT, JellySupport.textAreaHeight(JellySupport.MINIMUM_MINIMUM_HEIGHT - 1, null));
    }

}
