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

import hudson.model.Result;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BPInstanceConfig<PUBLISHER extends BapPublisher> implements Serializable {

    static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(BPInstanceConfig.class);

    private List<PUBLISHER> publishers;
    private boolean continueOnError;
    private boolean failOnError;
    private boolean alwaysPublishFromMaster;
    private String masterNodeName;
    private BPHostConfigurationAccess hostConfigurationAccess;

    public BPInstanceConfig() {}

	public BPInstanceConfig(List<PUBLISHER> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster, String masterNodeName) {
        setPublishers(publishers);
        this.continueOnError = continueOnError;
        this.failOnError = failOnError;
        this.alwaysPublishFromMaster = alwaysPublishFromMaster;
        this.masterNodeName = masterNodeName;
	}

    public List<PUBLISHER> getPublishers() {
        return publishers;
    }
    public void setPublishers(List<PUBLISHER> publishers) {
        if (publishers == null) {
            this.publishers = new ArrayList<PUBLISHER>();
        } else {
            this.publishers = publishers;
        }
    }

    public boolean isContinueOnError() { return continueOnError; }
    public void setContinueOnError(boolean continueOnError) { this.continueOnError = continueOnError; }

    public boolean isFailOnError() { return failOnError; }
    public void setFailOnError(boolean failOnError) { this.failOnError = failOnError; }

    public boolean isAlwaysPublishFromMaster() { return alwaysPublishFromMaster; }
    public void setAlwaysPublishFromMaster(boolean alwaysPublishFromMaster) { this.alwaysPublishFromMaster = alwaysPublishFromMaster; }

    public String getMasterNodeName() { return masterNodeName; }
    public void setMasterNodeName(String masterNodeName) { this.masterNodeName = masterNodeName; }
        
    public void setHostConfigurationAccess(BPHostConfigurationAccess hostConfigurationAccess) {
        this.hostConfigurationAccess = hostConfigurationAccess;
    }

    public BPHostConfiguration getConfiguration(String configName) {
        BPHostConfiguration config =  hostConfigurationAccess.getConfiguration(configName);
        if (config == null)
            throw new BapPublisherException(Messages.exception_failedToFindConfiguration(configName));
        return config;
    }

    private void fixMasterNodeName(BPBuildInfo buildInfo, BPBuildEnv buildEnv) {
        if (buildEnv != null)
            buildEnv.fixMasterNodeName(masterNodeName);
    }
    
    private void fixMasterNodeName(BPBuildInfo buildInfo) {
        fixMasterNodeName(buildInfo, buildInfo.getCurrentBuildEnv());
        fixMasterNodeName(buildInfo, buildInfo.getTargetBuildEnv());
    }
    
    public Result perform(BPBuildInfo buildInfo) {
        Result toReturn = Result.SUCCESS;
        Result onError = failOnError ? Result.FAILURE : Result.UNSTABLE;
        fixMasterNodeName(buildInfo);
        for (PUBLISHER publisher : publishers) {
            publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
            try {
                BPHostConfiguration hostConfig = getConfiguration(publisher.getConfigName());
                BPCallablePublisher callablePublisher = new BPCallablePublisher(publisher, hostConfig, buildInfo);
                if (alwaysPublishFromMaster)
                    callablePublisher.invoke(null, null);
                else
                    buildInfo.getBaseDirectory().act(callablePublisher);
            } catch (Exception e) {
                LOG.warn(Messages.log_exceptionInPerform(), e);
                buildInfo.getListener().error(e.getLocalizedMessage());
                if (continueOnError)
                    toReturn = toReturn.combine(onError);
                else
                    return onError;
            }
        }
		return toReturn;
    }
    
    protected HashCodeBuilder createHashCodeBuilder() {
        return addToHashCode(new HashCodeBuilder());
    }
    
    protected HashCodeBuilder addToHashCode(HashCodeBuilder builder) {
        return builder.append(publishers).append(continueOnError).append(failOnError)
            .append(alwaysPublishFromMaster).append(masterNodeName);
    }
    
    protected EqualsBuilder createEqualsBuilder(BPInstanceConfig that) {
        return addToEquals(new EqualsBuilder(), that);
    }
    
    protected EqualsBuilder addToEquals(EqualsBuilder builder, BPInstanceConfig that) {
        return builder.append(publishers, that.publishers)
            .append(continueOnError, that.continueOnError)
            .append(failOnError, that.failOnError)
            .append(masterNodeName, that.masterNodeName)
            .append(alwaysPublishFromMaster, that.alwaysPublishFromMaster);
    }
    
    protected ToStringBuilder addToToString(ToStringBuilder builder) {
        return builder.append("publishers", publishers)
            .append("continueOnError", continueOnError)
            .append("failOnError", failOnError)
            .append("masterNodeName", masterNodeName)
            .append("alwaysPublishFromMaster", alwaysPublishFromMaster);
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        return createEqualsBuilder((BPInstanceConfig) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
