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


import java.util.regex.Pattern;

import static jenkins.plugins.publish_over.JenkinsCapabilities.missing;
import static jenkins.plugins.publish_over.JenkinsCapabilities.CHECKBOX_WITH_TITLE;
import static jenkins.plugins.publish_over.JenkinsCapabilities.CHECKBOX_WITH_CSS_SPACE;

public class JellySupport {

    public static final int MINIMUM_MINIMUM_HEIGHT = 1;
    public static final int DEFAULT_MINIMUM_HEIGHT = 5;
    private static final Pattern LINE_END = Pattern.compile("\r?\n");

    public static final int textAreaHeight(final int minimum, final String content) {
        final int min = Math.max(minimum, MINIMUM_MINIMUM_HEIGHT);
        if (content == null) return min;
        return Math.max(min, LINE_END.split(content).length);
    }

    public static final boolean boxMissingTitle() {
        return missing(CHECKBOX_WITH_TITLE);
    }

    public static final boolean boxNeedsSpace() {
        return missing(CHECKBOX_WITH_CSS_SPACE);
    }

}
