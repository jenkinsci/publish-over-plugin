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
import jenkins.plugins.publish_over.helper.RandomFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals" })
public class FileFinderTest {

    private static final String FIND_ALL = "**/";
    private static final String FS = File.separator;

    @Rule // FindBugs: must be public for the @Rule to work
    public TemporaryFolder tmpDir = new TemporaryFolder();
    private FilePath baseDir;

    @Before
    public void setUp() throws Exception {
        baseDir = new FilePath(tmpDir.getRoot());
    }

    @Test public void noDirectoriesWhenFindEmptyDirectoriesFalse() throws Exception {
        assertTrue(new File(tmpDir.getRoot(), "ignoreMe").mkdir());
        final String expectedFileName = "expectMe.txt";
        new RandomFile(tmpDir.getRoot(), expectedFileName);

        final FileFinderResult result = invoke(FIND_ALL, null, false, false);
        assertFilePathArraysEqual(new String[]{expectedFileName}, result.getFiles());
        assertEquals(0, result.getDirectories().length);
    }

    @Test public void haveDirectoryWhenFindEmptyDirectoriesTrue() throws Exception {
        final String expectedDirName = "expectDir";
        final String expectedFileName = "expectMe.txt";
        assertTrue(new File(tmpDir.getRoot(), expectedDirName).mkdir());
        new RandomFile(tmpDir.getRoot(), expectedFileName);

        final FileFinderResult result = invoke(FIND_ALL, null, false, true);
        assertFilePathArraysEqual(new String[] {expectedFileName}, result.getFiles());
        assertFilePathArraysEqual(new String[]{expectedDirName}, result.getDirectories());
    }

    @Test public void canReduceDirectories() throws Exception {
        final String[] in = new String[] {"",
                                          "one",
                                          "two", "two" + FS + "a", "two" + FS + "a" + FS + "alpha",
                                          "three", "three" + FS + "a", "three" + FS + "b",
                                          "four"};
        final Set<String> expected = new HashSet<String>();
        expected.addAll(Arrays.asList(new String[]{"one",
                                                   "two" + FS + "a" + FS + "alpha",
                                                   "three" + FS + "a",
                                                   "three" + FS + "b",
                                                   "four"}));
        final Set<String> actualSet = new HashSet<String>();
        final String[] actual = FileFinder.reduce(in, in);
        actualSet.addAll(Arrays.asList(actual));
        assertEquals(expected, actualSet);
        assertEquals(expected.size(), actual.length);
    }

    @Test public void neverIncludeTheRootDirectory() throws Exception {
        final String[] onlyRoot = new String[] {""};
        assertArrayEquals(new String[0], FileFinder.reduce(onlyRoot, onlyRoot));
    }

    private void assertFilePathArraysEqual(final String[] expectedRelNames, final FilePath[] actual) {
        final FilePath[] expected = new FilePath[expectedRelNames.length];
        for (int i = 0; i < expected.length; i++)
            expected[i] = new FilePath(baseDir, expectedRelNames[i]);
        assertEquals(Arrays.asList(expected), Arrays.asList(actual));
    }

    private FileFinderResult invoke(final String includes, final String excludes, final boolean defaultExcludes,
                                    final boolean findEmptyDirectories) throws Exception {
        return baseDir.act(new FileFinder(includes, excludes, defaultExcludes, findEmptyDirectories));
    }

}
