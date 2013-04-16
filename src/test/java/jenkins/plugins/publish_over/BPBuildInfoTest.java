/*
 * The MIT License
 *
 * Copyright (C) 2012 by Anthony Robinson
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

import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.plugins.publish_over.helper.BPBuildInfoFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class BPBuildInfoTest {

    private FilePath baseDir = new FilePath(new File("baseDir"));
    private BPBuildInfo buildInfo;

    @Before
    public void setUp() throws Exception {
        final BPBuildEnv current = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo = new BPBuildInfo(TaskListener.NULL, "", new FilePath(new File("")), current, null);
        buildInfo.setEnvVars(new TreeMap<String, String>());
        buildInfo.setBaseDirectory(baseDir);
    }

    @Test public void testGetRelativePath() throws Exception {
        final String relPath = "./some/path/to/dir";
        final FilePath path = new FilePath(baseDir, relPath + "/filename.xxx");
        assertEquals(relPath.replace(".", ""), buildInfo.getRelativePathToFile(path, null));
    }

    @Test public void testGetRelativeDirectory() throws Exception {
        final String relDir = "./some/path/to/dir";
        final FilePath path = new FilePath(baseDir, relDir);
        assertEquals(relDir.replace(".", ""), buildInfo.getRelativeDir(path, null));
    }

}
