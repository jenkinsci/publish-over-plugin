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
import java.util.Set;

public class BPTransfer implements Serializable {

    static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(BPTransfer.class);

    private String remoteDirectory;
    private String sourceFiles;
    private String removePrefix;
    private boolean remoteDirectorySDF;
    private boolean flatten = true;

    public BPTransfer(final String sourceFiles, final String remoteDirectory, final String removePrefix,
                      final boolean remoteDirectorySDF, final boolean flatten) {
        this.sourceFiles = sourceFiles;
        this.remoteDirectory = remoteDirectory;
        this.removePrefix = removePrefix;
        this.remoteDirectorySDF = remoteDirectorySDF;
        this.flatten = flatten;
    }

    public String getRemoteDirectory() { return remoteDirectory; }
    public void setRemoteDirectory(final String remoteDirectory) { this.remoteDirectory = remoteDirectory; }

    public String getSourceFiles() { return sourceFiles; }
    public void setSourceFiles(final String sourceFiles) { this.sourceFiles = sourceFiles; }

    public String getRemovePrefix() { return removePrefix; }
    public void setRemovePrefix(final String removePrefix) { this.removePrefix = removePrefix; }

    public boolean isRemoteDirectorySDF() { return remoteDirectorySDF; }
    public void setRemoteDirectorySDF(final boolean remoteDirectorySDF) { this.remoteDirectorySDF = remoteDirectorySDF; }

    public boolean isFlatten() { return flatten; }
    public void setFlatten(final boolean flatten) { this.flatten = flatten; }

    public boolean hasConfiguredSourceFiles() {
        return Util.fixEmptyAndTrim(getSourceFiles()) != null;
    }

    public FilePath[] getSourceFiles(final BPBuildInfo buildInfo) throws IOException, InterruptedException {
        String expanded = Util.replaceMacro(sourceFiles, buildInfo.getEnvVars());
        if (LOG.isDebugEnabled())
            LOG.debug(Messages.log_sourceFiles(sourceFiles, expanded));
        return buildInfo.getBaseDirectory().list(expanded);
    }

    public int transfer(final BPBuildInfo buildInfo, final BPClient client) throws Exception {
        int transferred = 0;
        DirectoryMaker dirMaker = new DirectoryMaker(buildInfo, client);
        for (FilePath filePath : getSourceFiles(buildInfo)) {
            dirMaker.changeAndMakeDirs(filePath);
            transferFile(client, filePath);
            transferred++;
        }
        return transferred;
    }

    public void transferFile(final BPClient client, final FilePath filePath) throws Exception {
        InputStream is = filePath.read();
        try {
            client.transferFile(this, filePath, is);
        } finally {
            is.close();
        }
    }

    private class DirectoryMaker {

        private String previousPath = null;
        private BPBuildInfo buildInfo;
        private BPClient client;
        private String relativeRemoteSubDirectory;
        private Set<String> flattenedFileNames = new LinkedHashSet<String>();

        DirectoryMaker(final BPBuildInfo buildInfo, final BPClient client) throws IOException {
            this.buildInfo = buildInfo;
            this.client = client;
            if (flatten) {
                resetToSubDirectory();
            }
        }

        public void changeAndMakeDirs(final FilePath filePath) throws IOException, InterruptedException {
            if (flatten)
                assertNotDuplicateFileName(filePath);
            String relPath = buildInfo.getRelativePath(filePath, removePrefix);
            if (LOG.isDebugEnabled())
                LOG.debug(Messages.log_pathToFile(filePath.getName(), relPath));
            if (!relPath.equals(previousPath) && !flatten) {
                resetToSubDirectory();
                changeToTargetDirectory(filePath);
                previousPath = relPath;
            }
        }

        private void assertNotDuplicateFileName(final FilePath filePath) {
            String fileName = filePath.getName();
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
            if (relative.startsWith("/"))
                relative = relative.substring(1);
            if ("".equals(relative.trim()))
                relative = "";
            return relative;
        }

        private String buildTimeFormat(final String simpleDateFormatString) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormatString);
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
            String relativePath = buildInfo.getRelativePath(filePath, removePrefix);
            if (!"".equals(relativePath))
                chdir(relativePath);
        }

        private void resetToSubDirectory() throws IOException {
            client.changeToInitialDirectory();
            changeToSubDirectory();
        }

    }

    protected HashCodeBuilder createHashCodeBuilder() {
        return addToHashCode(new HashCodeBuilder());
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(sourceFiles).append(removePrefix).append(remoteDirectory)
            .append(remoteDirectorySDF).append(flatten);
    }

    protected EqualsBuilder createEqualsBuilder(final BPTransfer that) {
        return addToEquals(new EqualsBuilder(), that);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BPTransfer that) {
        return builder.append(sourceFiles, that.sourceFiles)
            .append(removePrefix, that.removePrefix)
            .append(remoteDirectory, that.remoteDirectory)
            .append(remoteDirectorySDF, that.remoteDirectorySDF)
            .append(flatten, that.flatten);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("sourceFiles", sourceFiles)
            .append("removePrefix", removePrefix)
            .append("remoteDirectory", remoteDirectory)
            .append("remoteDirectorySDF", remoteDirectorySDF)
            .append("flatten", flatten);
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return createEqualsBuilder((BPTransfer) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
