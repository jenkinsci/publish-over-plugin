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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public abstract class BPPlugin<PUBLISHER extends BapPublisher, CLIENT extends BPClient, COMMON_CONFIG> 
            extends Notifier implements BPHostConfigurationAccess<CLIENT, COMMON_CONFIG> {

    public static final String PROMOTION_JOB_TYPE = "hudson.plugins.promoted_builds.PromotionProcess";
    public static final String PROMOTION_CLASS_NAME = "hudson.plugins.promoted_builds.Promotion";

    private BPInstanceConfig delegate;
    private String consolePrefix;

	public BPPlugin(String consolePrefix, List<PUBLISHER> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster, String masterNodeName) {
		this.delegate = new BPInstanceConfig<PUBLISHER>(publishers, continueOnError, failOnError, alwaysPublishFromMaster, masterNodeName);
        delegate.setHostConfigurationAccess(this);
        this.consolePrefix = consolePrefix;
    }

	public List<BapPublisher> getPublishers() { return delegate.getPublishers(); }
	public void setPublishers(List<BapPublisher> publishers) { delegate.setPublishers(publishers); }

    public boolean isContinueOnError() { return delegate.isContinueOnError(); }
    public void setContinueOnError(boolean continueOnError) { delegate.setContinueOnError(continueOnError); }

    public boolean isFailOnError() { return delegate.isFailOnError(); }
    public void setFailOnError(boolean failOnError) { delegate.setFailOnError(failOnError); }

    public boolean isAlwaysPublishFromMaster() { return delegate.isAlwaysPublishFromMaster(); }
    public void setAlwaysPublishFromMaster(boolean alwaysPublishFromMaster) { delegate.setAlwaysPublishFromMaster(alwaysPublishFromMaster); }

    public String getMasterNodeName() { return delegate.getMasterNodeName(); }
    public void setMasterNodeName(String masterNodeName) { delegate.setMasterNodeName(masterNodeName); }
    
    public BPInstanceConfig getDelegate() { return delegate; }
    public void setDelegate(BPInstanceConfig delegate) { this.delegate = delegate; delegate.setHostConfigurationAccess(this); }

    public String getConsolePrefix() { return consolePrefix; }
    public void setConsolePrefix(String consolePrefix) { this.consolePrefix = consolePrefix; }

    public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

    private Map<String, String> getEnvironmentVariables(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            Map<String, String> env = build.getEnvironment(listener);
            env.putAll(build.getBuildVariables());
            return env;
        } catch (Exception e) {
            throw new RuntimeException(Messages.exception_failedToGetEnvVars(), e);
        }
    }

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        if (!isBuildGoodEnoughToRun(build, console)) return true;
        BPBuildEnv currentBuildEnv = new BPBuildEnv(getEnvironmentVariables(build, listener), build.getWorkspace(), build.getTimestamp());
        BPBuildEnv targetBuildEnv = null;
        if (PROMOTION_CLASS_NAME.equals(build.getClass().getCanonicalName())) {
            AbstractBuild<?, ?> promoted;
            try {
                Method getTarget = build.getClass().getMethod("getTarget", (Class<?>[])null);
                promoted = (AbstractBuild) getTarget.invoke(build, (Object[])null);
            } catch (Exception e) {
                throw new RuntimeException(Messages.exception_failedToGetPromotedBuild(), e);
            }
            targetBuildEnv = new BPBuildEnv(getEnvironmentVariables(promoted, listener), new FilePath(promoted.getArtifactsDir()), promoted.getTimestamp());
        }

        Result result = delegate.perform(new BPBuildInfo(listener, consolePrefix, Hudson.getInstance().getRootPath(), currentBuildEnv, targetBuildEnv));

        if (build.getResult() != null)
            build.setResult(result.combine(build.getResult()));
        else 
            build.setResult(result);
        return result.isBetterOrEqualTo(Result.UNSTABLE);
	}
    
    protected boolean isBuildGoodEnoughToRun(AbstractBuild<?, ?> build, PrintStream console) {
        if ((build.getResult() != null) && !build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
            console.println(Messages.console_notPerforming(build.getResult()));
            return false;
        }
        return true;
    }

    protected HashCodeBuilder createHashCodeBuilder() {
        return addToHashCode(new HashCodeBuilder());
    }

    protected HashCodeBuilder addToHashCode(HashCodeBuilder builder) {
        return builder.append(delegate).append(consolePrefix);
    }
    
    protected EqualsBuilder createEqualsBuilder(BPPlugin that) {
        return addToEquals(new EqualsBuilder(), that);
    }
    
    protected EqualsBuilder addToEquals(EqualsBuilder builder, BPPlugin that) {
        return builder.append(delegate, that.delegate)
            .append(consolePrefix, that.consolePrefix);
    }
    
    protected ToStringBuilder addToToString(ToStringBuilder builder) {
        return builder.append("consolePrefix", consolePrefix)
            .append("delegate", delegate);
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        return createEqualsBuilder((BPPlugin) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
