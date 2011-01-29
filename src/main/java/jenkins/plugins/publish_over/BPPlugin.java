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

public abstract class BPPlugin<PUBLISHER extends BapPublisher, CLIENT extends BPClient> extends Notifier implements BPHostConfigurationAccess<CLIENT> {

    private static final String PROMOTION_CLASS_NAME = "jenkins.plugins.promoted_builds.Promotion";

    private BPInstanceConfig delegate;
    private String consolePrefix;

	public BPPlugin(String consolePrefix, List<PUBLISHER> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster) {
		this.delegate = new BPInstanceConfig<PUBLISHER>(publishers, continueOnError, failOnError, alwaysPublishFromMaster);
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

    public BPInstanceConfig getDelegate() { return delegate; }
    public void setDelegate(BPInstanceConfig delegate) { this.delegate = delegate; }

    public String getConsolePrefix() { return consolePrefix; }
    public void setConsolePrefix(String consolePrefix) { this.consolePrefix = consolePrefix; }

    public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

    private Map<String, String> getEnvironmentVariables(AbstractBuild<?, ?> build, TaskListener listener) {
        return getEnvironmentVariables(build, listener, null);
    }

    private Map<String, String> getEnvironmentVariables(AbstractBuild<?, ?> build, TaskListener listener, Map<String, String> promotionVars) {
        Map<String, String> vars;
        try {
            vars = build.getEnvironment(listener);
        } catch (Exception e) {
            throw new RuntimeException(Messages.exception_failedToGetEnvVars(), e);
        }
        if (promotionVars != null) {
            String prefix = BPBuildInfo.PROMOTION_ENV_VARS_PREFIX;
            for (Map.Entry<String, String> entry : promotionVars.entrySet()) {
                vars.put(prefix + entry.getKey(), entry.getValue());
            }
        }
        return vars;
    }

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        if (!build.getResult().isBetterOrEqualTo(Result.UNSTABLE)) {
            console.println(Messages.console_notPerforming(build.getResult()));
            return true;
        }
        Map<String, String> envVars = getEnvironmentVariables(build, listener);
        FilePath baseDirectory;
        AbstractBuild<?, ?> buildToUse = build;
        if (PROMOTION_CLASS_NAME.equals(build.getClass().getCanonicalName())) {
            AbstractBuild<?, ?> promoted;
            try {
                Method getTarget = build.getClass().getMethod("getTarget", (Class<?>[])null);
                promoted = (AbstractBuild) getTarget.invoke(build, (Object[])null);
            } catch (Exception e) {
                throw new RuntimeException(Messages.exception_failedToGetPromotedBuild(), e);
            }
            buildToUse = promoted;
            baseDirectory = new FilePath(promoted.getArtifactsDir());
            envVars = getEnvironmentVariables(promoted, listener, envVars);
            console.println(Messages.console_promotion_yes());
        } else {
            baseDirectory = build.getWorkspace();
            console.println(Messages.console_promotion_no());
        }

        Result result = delegate.perform(new BPBuildInfo(envVars, baseDirectory, buildToUse.getTimestamp(), listener, consolePrefix));

        build.setResult(result.combine(build.getResult()));
        return result.isBetterOrEqualTo(Result.UNSTABLE);
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
