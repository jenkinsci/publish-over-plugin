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

import hudson.FilePath;
import hudson.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings({ "PMD.TooManyMethods", "PMD.SignatureDeclareThrowsException" })
public class BPTransfer implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(BPTransfer.class);

    private static FileFinderResult list(final FilePath base, final String includes, final String excludes,
                                         final boolean noDefaultExcludes, final boolean makeEmptyDirs) {
        try {
            return base.act(new FileFinder(includes, excludes, !noDefaultExcludes, makeEmptyDirs));
        } catch (IOException ioe) {
            throw new BapPublisherException(Messages.exception_invokeListNoDefaultExcludes(includes, excludes, noDefaultExcludes), ioe);
        } catch (InterruptedException ie) {
            throw new BapPublisherException(Messages.exception_invokeListNoDefaultExcludes(includes, excludes, noDefaultExcludes), ie);
        }
    }

    private final String remoteDirectory;
    private final String sourceFiles;
    private final String excludes;
    private final String removePrefix;
    private final boolean remoteDirectorySDF;
    private final boolean flatten;
    private final boolean cleanRemote;
    private final boolean noDefaultExcludes;
    private final boolean makeEmptyDirs;

    // @TODO can now test excludes and default excludes
    BPTransfer(final String sourceFiles, final String remoteDirectory, final String removePrefix,
                      final boolean remoteDirectorySDF, final boolean flatten) {
        this(sourceFiles, null, remoteDirectory, removePrefix, remoteDirectorySDF, flatten, false, false, false);
    }

    public BPTransfer(final String sourceFiles, final String excludes, final String remoteDirectory, final String removePrefix,
                      final boolean remoteDirectorySDF, final boolean flatten) {
        this(sourceFiles, excludes, remoteDirectory, removePrefix, remoteDirectorySDF, flatten, false, false, false);
    }

    public BPTransfer(final String sourceFiles, final String excludes, final String remoteDirectory, final String removePrefix,
                      final boolean remoteDirectorySDF, final boolean flatten, final boolean cleanRemote,
                      final boolean noDefaultExcludes, final boolean makeEmptyDirs) {
        this.sourceFiles = sourceFiles;
        this.excludes = excludes;
        this.remoteDirectory = remoteDirectory;
        this.removePrefix = removePrefix;
        this.remoteDirectorySDF = remoteDirectorySDF;
        this.flatten = flatten;
        this.cleanRemote = cleanRemote;
        this.noDefaultExcludes = noDefaultExcludes;
        this.makeEmptyDirs = makeEmptyDirs;
    }

    public String getRemoteDirectory() { return remoteDirectory; }

    public String getSourceFiles() { return sourceFiles; }

    public String getExcludes() { return excludes; }

    public String getRemovePrefix() { return removePrefix; }

    public boolean isRemoteDirectorySDF() { return remoteDirectorySDF; }

    public boolean isFlatten() { return flatten; }

    public boolean isCleanRemote() { return cleanRemote; }

    public boolean isNoDefaultExcludes() { return noDefaultExcludes; }

    public boolean isMakeEmptyDirs() { return makeEmptyDirs; }

    public boolean hasConfiguredSourceFiles() {
        return Util.fixEmptyAndTrim(getSourceFiles()) != null;
    }

    public FileFinderResult getSourceFiles(final BPBuildInfo buildInfo) throws IOException, InterruptedException {
        final String expanded = Util.replaceMacro(sourceFiles, buildInfo.getEnvVars());
        final String expandedExcludes = Util.fixEmptyAndTrim(Util.replaceMacro(excludes, buildInfo.getEnvVars()));
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.log_sourceFiles(sourceFiles, expanded));
            if (expandedExcludes != null)
                LOG.debug(Messages.log_excludes(excludes, expandedExcludes));
        }
        return list(buildInfo.getBaseDirectory(), expanded, expandedExcludes, noDefaultExcludes, makeEmptyDirs);
    }

    private void assertBaseDirectoryExists(final BPBuildInfo buildInfo) throws Exception {
        if (!buildInfo.getBaseDirectory().exists())
            throw new BapPublisherException(Messages.exception_baseDirectoryNotExist());
    }

    public int transfer(final BPBuildInfo buildInfo, final BPClient client) throws Exception {
        assertBaseDirectoryExists(buildInfo);
        return transfer(buildInfo, client, TransferState.create(getSourceFiles(buildInfo)));
    }

    public int transfer(final BPBuildInfo buildInfo, final BPClient client, final TransferState state) {
        try {
            final DirectoryMaker dirMaker = new DirectoryMaker(buildInfo, client);
            if (cleanRemote && !state.doneCleaning) {
                dirMaker.resetToSubDirectory();
                client.deleteTree();
                state.doneCleaning = true;
            }
            while (state.transferred < state.sourceFiles.length) {
                dirMaker.changeAndMakeDirs(state.sourceFiles[state.transferred], false);
                transferFile(client, state.sourceFiles[state.transferred]);
                state.transferred++;
            }
            while (state.dirsMade < state.emptyDirs.length) {
                dirMaker.changeAndMakeDirs(state.emptyDirs[state.dirsMade], true);
                state.dirsMade++;
            }
        } catch (Exception e) {
            throw new BapTransferException(e, state);
        }
        return state.transferred;
    }

    public void transferFile(final BPClient client, final FilePath filePath) throws Exception {
        final InputStream inputStream = filePath.read();
        try {
            client.transferFile(this, filePath, inputStream);
        } finally {
            inputStream.close();
        }
    }

    private class DirectoryMaker {

        private final BPBuildInfo buildInfo;
        private final BPClient client;
        private final Set<String> flattenedFileNames = new LinkedHashSet<String>();
        private boolean flattenResetCompleted;
        private String previousPath;
        private String relativeRemoteSubDirectory;

        DirectoryMaker(final BPBuildInfo buildInfo, final BPClient client) throws IOException {
            this.buildInfo = buildInfo;
            this.client = client;
        }

        public void changeAndMakeDirs(final FilePath filePath, final boolean isDirectory) throws IOException, InterruptedException {
            if (flatten) {
                assertNotDuplicateFileName(filePath);
                if (!flattenResetCompleted) {
                    // Only create target directory when there is a file to store
                    resetToSubDirectory();
                    flattenResetCompleted = true;
                }
            }
            final String relPath = isDirectory ? buildInfo.getRelativeDir(filePath, removePrefix)
                                               : buildInfo.getRelativePathToFile(filePath, removePrefix);
            if (LOG.isDebugEnabled())
                LOG.debug(Messages.log_pathToFile(filePath.getName(), relPath));
            if (!relPath.equals(previousPath) && !flatten) {
                resetToSubDirectory();
                changeToTargetDirectory(filePath);
                previousPath = relPath;
            }
        }

        private void assertNotDuplicateFileName(final FilePath filePath) {
            final String fileName = filePath.getName();
            if (flattenedFileNames.contains(fileName))
                throw new BapPublisherException(Messages.exception_flattenModeDuplicateFileName(fileName));
            flattenedFileNames.add(fileName);
        }

        private void changeToSubDirectory() throws IOException {
            if (relativeRemoteSubDirectory == null) {
                relativeRemoteSubDirectory = getRelativeRemoteDirectory();
            }
            if (!"".equals(relativeRemoteSubDirectory)) {
                chdir(relativeRemoteSubDirectory);
            }
        }

        private String getRelativeRemoteDirectory() {
            String relative = remoteDirectory;
            if (relative == null)
                return "";
            relative = Util.replaceMacro(relative, buildInfo.getEnvVars());
            relative = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(relative));
            if (relative == null)
                return "";
            if (remoteDirectorySDF)
                relative = buildTimeFormat(relative);
            relative = Util.fixEmptyAndTrim(relative);
            if (relative == null)
                return "";
            if (relative.charAt(0) == '/')
                return relative.substring(1);
            return relative;
        }

        private String buildTimeFormat(final String simpleDateFormatString) {
            try {
                // get locale from master?
                final SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormatString, Locale.getDefault());
                return sdf.format(buildInfo.getBuildTime().getTime());
            } catch (IllegalArgumentException iae) {
                throw new BapPublisherException(Messages.exception_badDateFormat(simpleDateFormatString, iae.getLocalizedMessage()), iae);
            }
        }

        private String[] getDirectories(final String directoryPath) {
            if (directoryPath.contains("/")) {
                return directoryPath.split("/");
            } else if (directoryPath.contains("\\")) {
                return directoryPath.split("\\\\");
            }
            return new String[]{directoryPath};
        }

        private void chdir(final String directory) throws IOException {
            if (!changeOrMakeAndChangeDirectory(directory)) {
                for (String dir : getDirectories(directory)) {
                    if (!changeOrMakeAndChangeDirectory(dir)) {
                        throw new BapPublisherException(Messages.exception_failedToCreateDirectory(dir));
                    }
                }
            }
        }

        private boolean changeOrMakeAndChangeDirectory(final String directory) throws IOException {
            if (client.changeDirectory(directory))
                return true;
            return client.makeDirectory(directory) && client.changeDirectory(directory);
        }

        private void changeToTargetDirectory(final FilePath filePath) throws IOException, InterruptedException {
            if (flatten)
                return;
            final String relativePath = buildInfo.getRelativePathToFile(filePath, removePrefix);
            if (!"".equals(relativePath))
                chdir(relativePath);
        }

        private void resetToSubDirectory() throws IOException {
            client.changeToInitialDirectory();
            changeToSubDirectory();
        }

    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(sourceFiles).append(removePrefix).append(remoteDirectory)
            .append(remoteDirectorySDF).append(flatten).append(cleanRemote).append(excludes).append(noDefaultExcludes)
            .append(makeEmptyDirs);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BPTransfer that) {
        return builder.append(sourceFiles, that.sourceFiles)
            .append(removePrefix, that.removePrefix)
            .append(excludes, that.excludes)
            .append(remoteDirectory, that.remoteDirectory)
            .append(remoteDirectorySDF, that.remoteDirectorySDF)
            .append(flatten, that.flatten)
            .append(cleanRemote, that.cleanRemote)
            .append(noDefaultExcludes, that.noDefaultExcludes)
            .append(makeEmptyDirs, that.makeEmptyDirs);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("sourceFiles", sourceFiles)
            .append("excludes", excludes)
            .append("removePrefix", removePrefix)
            .append("remoteDirectory", remoteDirectory)
            .append("remoteDirectorySDF", remoteDirectorySDF)
            .append("flatten", flatten)
            .append("cleanRemote", cleanRemote)
            .append("noDefaultExcludes", noDefaultExcludes)
            .append("makeEmptyDirs", makeEmptyDirs);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BPTransfer) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public static final class TransferState implements Serializable {
        private static final long serialVersionUID = 1L;
        private final FilePath[] sourceFiles;
        private final FilePath[] emptyDirs;
        private int transferred;
        private int dirsMade;
        private boolean doneCleaning;
        private TransferState(final FileFinderResult sources) {
            this.sourceFiles = sources.getFiles();
            this.emptyDirs = sources.getDirectories();
        }
        protected static TransferState create(final FileFinderResult sources) {
            return new TransferState(sources);
        }
    }

}
