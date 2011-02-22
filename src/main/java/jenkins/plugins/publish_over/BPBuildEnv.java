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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

public class BPBuildEnv implements Serializable {

    static final long serialVersionUID = 1L;
    private static final Log LOG = LogFactory.getLog(BPBuildEnv.class);
    public static final String ENV_NODE_NAME = "NODE_NAME";
    public static final String ENV_JOB_NAME = "JOB_NAME";
    public static final String ENV_BUILD_NUMBER = "BUILD_NUMBER";

    private TreeMap<String, String> envVars;
    private FilePath baseDirectory;
    private Calendar buildTime;

    public BPBuildEnv() { }

    public BPBuildEnv(final TreeMap<String, String> envVars, final FilePath baseDirectory, final Calendar buildTime) {
        this.envVars = envVars;
        this.baseDirectory = baseDirectory;
        this.buildTime = buildTime;
    }

    public TreeMap<String, String> getEnvVars() { return envVars; }
    public void setEnvVars(final TreeMap<String, String> envVars) { this.envVars = envVars; }

    public FilePath getBaseDirectory() { return baseDirectory; }
    public void setBaseDirectory(final FilePath baseDirectory) { this.baseDirectory = baseDirectory; }

    public Calendar getBuildTime() { return buildTime; }
    public void setBuildTime(final Calendar buildTime) { this.buildTime = buildTime; }

    public TreeMap<String, String> getEnvVarsWithPrefix(final String prefix) {
        TreeMap<String, String> prefixed = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            prefixed.put(prefix + entry.getKey(), entry.getValue());
        }
        return prefixed;
    }

    public void logEnvVars() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.log_envVars_head());
            StringBuilder builder = new StringBuilder("\n");
            for (Map.Entry var : envVars.entrySet()) {
                builder.append(Messages.log_envVars_pair(var.getKey(), var.getValue()));
                builder.append("\n");
            }
            LOG.debug(builder.toString());
        }
    }

    public void fixMasterNodeName(final String masterNodeName) {
        fixEmptyButNotMissingEnvVar(ENV_NODE_NAME, masterNodeName);
    }

    private void fixEmptyButNotMissingEnvVar(final String envVarName, final String replacement) {
        if (Util.fixEmptyAndTrim(replacement) == null) return;
        if (!envVars.containsKey(envVarName)) return;
        if (Util.fixEmptyAndTrim(envVars.get(envVarName)) == null)
            envVars.put(envVarName, replacement);
    }

    public String getNormalizedBaseDirectory() {
        try {
            return baseDirectory.toURI().normalize().getPath();
        } catch (Exception e) {
            throw new RuntimeException(Messages.exception_normalizeDirectory(baseDirectory), e);
        }
    }

    private String safeGetNormalizedBaseDirectory() {
        if (baseDirectory == null) return null;
        try {
            return getNormalizedBaseDirectory();
        } catch (RuntimeException re) {
            return re.getLocalizedMessage();
        }
    }

    private String safeGetBuildTime() {
        if (buildTime == null) return null;
        try {
            return DateFormat.getDateTimeInstance().format(buildTime.getTime());
        } catch (RuntimeException re) {
            return re.getLocalizedMessage();
        }
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        if (envVars != null) {
            builder.append(ENV_JOB_NAME, envVars.get(ENV_JOB_NAME))
               .append(ENV_BUILD_NUMBER, envVars.get(ENV_BUILD_NUMBER));
        }
        return builder.append("baseDirectory", safeGetNormalizedBaseDirectory())
               .append("buildTime", safeGetBuildTime());
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
