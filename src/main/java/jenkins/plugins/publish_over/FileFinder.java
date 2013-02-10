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
import hudson.remoting.VirtualChannel;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class FileFinder implements FilePath.FileCallable<FileFinderResult> {

    private static final long serialVersionUID = 1L;

    private final String includes;
    private final String excludes;
    private final boolean defaultExcludes;
    private final boolean findEmptyDirectories;

    public FileFinder(final String includes, final String excludes, final boolean defaultExcludes, final boolean findEmptyDirectories) {
        this.includes = includes;
        this.excludes = excludes;
        this.defaultExcludes = defaultExcludes;
        this.findEmptyDirectories = findEmptyDirectories;
    }

    public FileFinderResult invoke(final File file, final VirtualChannel virtualChannel) throws IOException, InterruptedException {
        final DirectoryScanner scanner = createDirectoryScanner(file, includes, excludes, defaultExcludes);
        final String[] includedFiles = scanner.getIncludedFiles();
        final FilePath[] files = toFilePathArray(file, includedFiles);
        FilePath[] dirs = new FilePath[0];
        if (findEmptyDirectories) {
            final String[] allDirs = scanner.getIncludedDirectories();
            final String[] onlyLeaf = reduce(allDirs, allDirs);
            dirs = toFilePathArray(file, reduce(onlyLeaf, includedFiles));
        }
        return new FileFinderResult(files, dirs);
    }

    static DirectoryScanner createDirectoryScanner(final File dir, final String includes, final String excludes, final boolean defaultExcludes) throws IOException {
        final FileSet fs = new FileSet();
        fs.setDir(dir);
        fs.setProject(new Project());
        fs.setIncludes(includes);
        if (excludes != null)
            fs.setExcludes(excludes);
        fs.setDefaultexcludes(defaultExcludes);
        return fs.getDirectoryScanner();
    }

    static String[] reduce(final String[] directories, final String[] paths) {
        final HashSet<String> result = new HashSet(Arrays.asList(directories));
        final LinkedHashSet<String> pathSet = new LinkedHashSet(Arrays.asList(paths));
        result.remove("");
        pathSet.remove("");
        for (final String dir : directories)
            for (final String potential : pathSet)
                if (potential.startsWith(dir + File.separator)) {
                    result.remove(dir);
                    pathSet.remove(dir);
                    break;
                }
        return result.toArray(new String[result.size()]);
    }

    private static FilePath[] toFilePathArray(final File file, final String[] includedFiles) {
        final FilePath[] filePaths = new FilePath[includedFiles.length];
        for (int i = 0; i < filePaths.length; i++)
            filePaths[i] = new FilePath(new File(file, includedFiles[i]));
        return filePaths;
    }

}


