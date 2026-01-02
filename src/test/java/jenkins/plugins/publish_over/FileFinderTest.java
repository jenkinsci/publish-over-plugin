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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.DirectoryScannerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals" })
class FileFinderTest {

    private static final String FIND_ALL = "**/";
    private static final String FS = File.separator;
    private static final String PATTERN_TEST_STRING = " one,two three, four,,,    ,, five";
    private static final String PATTERN_TEST_STRING_COMMA_SEP = "pattern one,pattern two";

    @TempDir // FindBugs: must be public for the @Rule to work
    private File tmpDir;
    private FilePath baseDir;

    @BeforeEach
    void beforeEach() {
        baseDir = new FilePath(tmpDir);
    }

    @Test
    void noDirectoriesWhenFindEmptyDirectoriesFalse() throws Exception {
        assertTrue(new File(tmpDir, "ignoreMe").mkdir());
        final String expectedFileName = "expectMe.txt";
        new RandomFile(tmpDir, expectedFileName);

        final FileFinderResult result = invoke(FIND_ALL, null, false, false);
        assertFilePathArraysEqual(new String[]{expectedFileName}, result.getFiles());
        assertEquals(0, result.getDirectories().length);
    }

    @Test
    void haveDirectoryWhenFindEmptyDirectoriesTrue() throws Exception {
        final String expectedDirName = "expectDir";
        final String expectedFileName = "expectMe.txt";
        assertTrue(new File(tmpDir, expectedDirName).mkdir());
        new RandomFile(tmpDir, expectedFileName);

        final FileFinderResult result = invoke(FIND_ALL, null, false, true);
        assertFilePathArraysEqual(new String[] {expectedFileName}, result.getFiles());
        assertFilePathArraysEqual(new String[]{expectedDirName}, result.getDirectories());
    }

    @Test
    void canReduceDirectories() {
        final String[] in = new String[] {"",
                                          "one",
                                          "two", "two" + FS + "a", "two" + FS + "a" + FS + "alpha",
                                          "three", "three" + FS + "a", "three" + FS + "b",
                                          "four"};
        final Set<String> expected = new HashSet<>(Arrays.asList("one",
                "two" + FS + "a" + FS + "alpha",
                "three" + FS + "a",
                "three" + FS + "b",
                "four"));
        final String[] actual = FileFinder.reduce(in, in);
        final Set<String> actualSet = new HashSet<>(Arrays.asList(actual));
        assertEquals(expected, actualSet);
        assertEquals(expected.size(), actual.length);
    }

    @Test
    void neverIncludeTheRootDirectory() {
        final String[] onlyRoot = new String[] {""};
        assertArrayEquals(new String[0], FileFinder.reduce(onlyRoot, onlyRoot));
    }

    @Test
    void defaultIncludesPatternSeparatorsAreCommaAndSpace() throws Exception {
        final String includes = PATTERN_TEST_STRING;
        final DirectoryScanner ds = FileFinder.createDirectoryScanner(tmpDir, includes, null, false, FileFinder.DEFAULT_PATTERN_SEPARATOR);
        final DirectoryScannerAccessor dsAccess = new DirectoryScannerAccessor(ds);
        assertArrayEquals(new String[] {"one", "two", "three", "four", "five"}, dsAccess.getIncludes());
    }

    @Test
    void defaultExcludesPatternSeparatorsAreCommaAndSpace() throws Exception {
        final String excludes = PATTERN_TEST_STRING;
        final DirectoryScanner ds = FileFinder.createDirectoryScanner(tmpDir, "", excludes, false, FileFinder.DEFAULT_PATTERN_SEPARATOR);
        final DirectoryScannerAccessor dsAccess = new DirectoryScannerAccessor(ds);
        assertArrayEquals(new String[] {"one", "two", "three", "four", "five"}, dsAccess.getExcludes());
    }

    @Test
    void setPatternSeparatorToEnablePatternsWithSpaces() throws Exception {
        final String includes = PATTERN_TEST_STRING_COMMA_SEP;
        final String excludes = PATTERN_TEST_STRING_COMMA_SEP;
        final DirectoryScanner ds = FileFinder.createDirectoryScanner(tmpDir, includes, excludes, false, ",");
        final DirectoryScannerAccessor dsAccess = new DirectoryScannerAccessor(ds);
        assertArrayEquals(new String[] {"pattern one", "pattern two"}, dsAccess.getIncludes());
        assertArrayEquals(new String[] {"pattern one", "pattern two"}, dsAccess.getExcludes());
    }

    private void assertFilePathArraysEqual(final String[] expectedRelNames, final FilePath[] actual) {
        final FilePath[] expected = new FilePath[expectedRelNames.length];
        for (int i = 0; i < expected.length; i++)
            expected[i] = new FilePath(baseDir, expectedRelNames[i]);
        assertEquals(Arrays.asList(expected), Arrays.asList(actual));
    }

    private FileFinderResult invoke(final String includes, final String excludes, final boolean defaultExcludes,
                                    final boolean findEmptyDirectories) throws Exception {
        return baseDir.act(new FileFinder(includes, excludes, defaultExcludes, findEmptyDirectories, null));
    }

}
