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
import hudson.model.TaskListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.IOException;
import java.io.InputStream;

public class BPBuildInfo extends BPBuildEnv {

    private static final long serialVersionUID = 1L;
    public static final String PROMOTION_ENV_VARS_PREFIX = "promotion_";

    private FilePath configDir;
    private TaskListener listener;
    private boolean verbose;
    private String consoleMsgPrefix;
    private BPBuildEnv currentBuildEnv;
    private BPBuildEnv targetBuildEnv;

    public BPBuildInfo() { }

    public BPBuildInfo(final TaskListener listener, final String consoleMsgPrefix, final FilePath configDir,
                       final BPBuildEnv currentBuildEnv, final BPBuildEnv targetBuildEnv) {
        this.listener = listener;
        this.consoleMsgPrefix = consoleMsgPrefix;
        this.configDir = configDir;
        this.currentBuildEnv = currentBuildEnv;
        this.targetBuildEnv = targetBuildEnv;
    }

    public FilePath getConfigDir() { return configDir; }
    public void setConfigDir(final FilePath configDir) { this.configDir = configDir; }

    public TaskListener getListener() { return listener; }
    public void setListener(final TaskListener listener) { this.listener = listener; }

    public String getConsoleMsgPrefix() { return consoleMsgPrefix; }
    public void setConsoleMsgPrefix(final String consoleMsgPrefix) { this.consoleMsgPrefix = consoleMsgPrefix; }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(final boolean verbose) { this.verbose = verbose; }

    public BPBuildEnv getCurrentBuildEnv() { return currentBuildEnv; }
    public void setCurrentBuildEnv(final BPBuildEnv currentBuildEnv) { this.currentBuildEnv = currentBuildEnv; }

    public BPBuildEnv getTargetBuildEnv() { return targetBuildEnv; }
    public void setTargetBuildEnv(final BPBuildEnv targetBuildEnv) { this.targetBuildEnv = targetBuildEnv; }

    public byte[] readFileFromMaster(final String filePath) {
        FilePath file = configDir.child(filePath);
        InputStream is = null;
        try {
            is = file.read();
            return IOUtils.toByteArray(is);
        } catch (IOException ioe) {
            throw new BapPublisherException(Messages.exception_readFile(filePath, ioe.getLocalizedMessage()), ioe);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public String getRelativePath(final FilePath filePath, final String removePrefix) throws IOException, InterruptedException {
        String normalizedPathToFile = filePath.toURI().normalize().getPath();
        String relativePathToFile = normalizedPathToFile.replace(getNormalizedBaseDirectory(), "");
        if (Util.fixEmptyAndTrim(removePrefix) != null) {
            String expanded = Util.fixEmptyAndTrim(Util.replaceMacro(removePrefix.trim(), getEnvVars()));
            if (expanded != null) {
                String toRemove = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(expanded + "/"));
                if (toRemove != null) {
                    if (toRemove.startsWith("/"))
                        toRemove = toRemove.substring(1);
                    if (!relativePathToFile.startsWith(toRemove)) {
                        throw new BapPublisherException(Messages.exception_removePrefix_noMatch(relativePathToFile, toRemove));
                    }
                    relativePathToFile = relativePathToFile.substring(toRemove.length());
                }
            }
        }
        int lastDirIdx = relativePathToFile.lastIndexOf("/");
        if (lastDirIdx == -1)
            return "";
        else
            return relativePathToFile.substring(0, lastDirIdx);
    }

    public void println(final String message) {
        if (listener != null) {
            listener.getLogger().println(consoleMsgPrefix + message);
        }
    }

    public void printIfVerbose(final String message) {
        if (verbose) {
            println(message);
        }
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE))
            .append("currentBuildEnv", currentBuildEnv)
            .append("targetBuildEnv", targetBuildEnv)
            .toString();
    }

}
