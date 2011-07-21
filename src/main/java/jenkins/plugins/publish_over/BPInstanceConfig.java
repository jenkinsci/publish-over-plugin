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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

// serializable + only actually 4 "real" methods in here all rest accessors and boiler str/has/eq
@SuppressWarnings({ "PMD.LooseCoupling", "PMD.TooManyMethods" })
public class BPInstanceConfig<PUBLISHER extends BapPublisher> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(BPInstanceConfig.class.getName());
    public static final String DEFAULT_MASTER_NODE_NAME = "master";

    private ArrayList<PUBLISHER> publishers;
    private boolean continueOnError;
    private boolean failOnError;
    private boolean alwaysPublishFromMaster;
    private String masterNodeName;
    private BPHostConfigurationAccess hostConfigurationAccess;
    private ParamPublish paramPublish;

    public BPInstanceConfig() { }

    public BPInstanceConfig(final ArrayList<PUBLISHER> publishers, final boolean continueOnError, final boolean failOnError,
                            final boolean alwaysPublishFromMaster, final String masterNodeName, final ParamPublish paramPublish) {
        setPublishers(publishers);
        this.continueOnError = continueOnError;
        this.failOnError = failOnError;
        this.alwaysPublishFromMaster = alwaysPublishFromMaster;
        this.masterNodeName = masterNodeName;
        this.paramPublish = paramPublish;
    }

    public final ArrayList<PUBLISHER> getPublishers() {
        return publishers;
    }
    public final void setPublishers(final ArrayList<PUBLISHER> publishers) {
        if (publishers == null) {
            this.publishers = new ArrayList<PUBLISHER>();
        } else {
            this.publishers = publishers;
        }
    }

    public final boolean isContinueOnError() { return continueOnError; }
    public final void setContinueOnError(final boolean continueOnError) { this.continueOnError = continueOnError; }

    public final boolean isFailOnError() { return failOnError; }
    public final void setFailOnError(final boolean failOnError) { this.failOnError = failOnError; }

    public final boolean isAlwaysPublishFromMaster() { return alwaysPublishFromMaster; }
    public final void setAlwaysPublishFromMaster(final boolean alwaysPublishFromMaster) {
        this.alwaysPublishFromMaster = alwaysPublishFromMaster;
    }

    public final String getMasterNodeName() { return masterNodeName; }
    public final void setMasterNodeName(final String masterNodeName) { this.masterNodeName = masterNodeName; }

    public final void setHostConfigurationAccess(final BPHostConfigurationAccess hostConfigurationAccess) {
        this.hostConfigurationAccess = hostConfigurationAccess;
    }

    public ParamPublish getParamPublish() {
        return paramPublish;
    }

    public BPHostConfiguration getConfiguration(final String configName) {
        final BPHostConfiguration config = hostConfigurationAccess.getConfiguration(configName);
        if (config == null)
            throw new BapPublisherException(Messages.exception_failedToFindConfiguration(configName));
        return config;
    }

    private void fixMasterNodeNameInEnv(final BPBuildEnv buildEnv) {
        if (buildEnv != null)
            buildEnv.fixMasterNodeName(masterNodeName);
    }

    private void fixMasterNodeName(final BPBuildInfo buildInfo) {
        fixMasterNodeNameInEnv(buildInfo.getCurrentBuildEnv());
        fixMasterNodeNameInEnv(buildInfo.getTargetBuildEnv());
    }

    public final Result perform(final BPBuildInfo buildInfo) {
        Result toReturn = Result.SUCCESS;
        final Result onError = failOnError ? Result.FAILURE : Result.UNSTABLE;
        if (masterNodeName != null) fixMasterNodeName(buildInfo);
        PubSelector selector = null;
        try {
            selector = createSelector(buildInfo);
        } catch (BapPublisherException bpe) {
            LOGGER.log(Level.WARNING, bpe.getLocalizedMessage(), bpe);
            buildInfo.getListener().error(bpe.getLocalizedMessage());
            return onError;
        }
        for (PUBLISHER publisher : publishers) {
            publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
            if (!selector.selected(publisher)) continue;
            try {
                final BPHostConfiguration hostConfig = getConfiguration(publisher.getConfigName());
                final BPCallablePublisher callablePublisher = new BPCallablePublisher(publisher, hostConfig, buildInfo);
                if (alwaysPublishFromMaster)
                    callablePublisher.invoke(null, null);
                else
                    buildInfo.getBaseDirectory().act(callablePublisher);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, Messages.log_exceptionInPerform(), e);
                buildInfo.getListener().error(e.getLocalizedMessage());
                if (continueOnError)
                    toReturn = toReturn.combine(onError);
                else
                    return onError;
            }
        }
        return toReturn;
    }

    private PubSelector createSelector(final BPBuildInfo buildInfo) {
        if (paramPublish == null)
            return SelectAllPubSelector.SELECT_ALL;
        return paramPublish.createSelector(buildInfo);
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(publishers).append(continueOnError).append(failOnError)
            .append(alwaysPublishFromMaster).append(masterNodeName).append(paramPublish);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BPInstanceConfig that) {
        return builder.append(publishers, that.publishers)
            .append(continueOnError, that.continueOnError)
            .append(failOnError, that.failOnError)
            .append(masterNodeName, that.masterNodeName)
            .append(alwaysPublishFromMaster, that.alwaysPublishFromMaster)
            .append(paramPublish, that.paramPublish);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("publishers", publishers)
            .append("continueOnError", continueOnError)
            .append("failOnError", failOnError)
            .append("masterNodeName", masterNodeName)
            .append("alwaysPublishFromMaster", alwaysPublishFromMaster)
            .append("paramPublish", paramPublish);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BPInstanceConfig) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
