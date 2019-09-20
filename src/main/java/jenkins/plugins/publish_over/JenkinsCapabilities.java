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

import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

public class JenkinsCapabilities {

    public static final VersionNumber CHECKBOX_WITH_TITLE = new VersionNumber("1.389");

    public static final VersionNumber SIMPLE_DESCRIPTOR_SELECTOR = new VersionNumber("1.391");

    public static final VersionNumber VALIDATE_FILE_ON_MASTER_FROM_GLOBAL_CFG = new VersionNumber("1.399");

    public static final VersionNumber CHECKBOX_WITH_CSS_SPACE = new VersionNumber("1.406");

    public static final VersionNumber MASTER_HAS_NODE_NAME = new VersionNumber("1.414");

    public static final boolean available(final VersionNumber version) {
        return !missing(version);
    }

    public static final boolean missing(final VersionNumber version) {
        VersionNumber jenkinsVersion = Jenkins.getVersion();
        return jenkinsVersion == null || jenkinsVersion.isOlderThan(version);
    }

}
