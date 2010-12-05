package hudson.plugins.bap_publisher;

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
import java.util.Map;

public class BPInstanceConfig<PUBLISHER extends BapPublisher> implements Serializable {

    static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(BPInstanceConfig.class);

    private List<PUBLISHER> publishers;
    private boolean continueOnError;
    private boolean failOnError;
    private boolean alwaysPublishFromMaster;
    private BPHostConfigurationAccess hostConfigurationAccess;

    public BPInstanceConfig() {}

	public BPInstanceConfig(List<PUBLISHER> publishers, boolean continueOnError, boolean failOnError, boolean alwaysPublishFromMaster) {
        setPublishers(publishers);
        this.continueOnError = continueOnError;
        this.failOnError = failOnError;
        this.alwaysPublishFromMaster = alwaysPublishFromMaster;
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

    public void setHostConfigurationAccess(BPHostConfigurationAccess hostConfigurationAccess) {
        this.hostConfigurationAccess = hostConfigurationAccess;
    }

    public BPHostConfiguration getConfiguration(String configName) {
        BPHostConfiguration config =  hostConfigurationAccess.getConfiguration(configName);
        if (config == null)
            throw new BapPublisherException(Messages.exception_failedToFindConfiguration(configName));
        return config;
    }

    private void logEnvVars(BPBuildInfo buildInfo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(Messages.log_envVars_head());
            StringBuilder builder = new StringBuilder("\n");
            for(Map.Entry var : buildInfo.getEnvVars().entrySet()) {
                builder.append(Messages.log_envVars_pair(var.getKey(),var.getValue()));
                builder.append("\n");
            }
            LOG.debug(builder.toString());
        }
    }

    public Result perform(BPBuildInfo buildInfo) {
        Result toReturn = Result.SUCCESS;
        Result onError = failOnError ? Result.FAILURE : Result.UNSTABLE;
        logEnvVars(buildInfo);
        for (PUBLISHER publisher : publishers) {
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
        return builder.append(publishers).append(continueOnError).append(failOnError).append(alwaysPublishFromMaster);
    }
    
    protected EqualsBuilder createEqualsBuilder(BPInstanceConfig that) {
        return addToEquals(new EqualsBuilder(), that);
    }
    
    protected EqualsBuilder addToEquals(EqualsBuilder builder, BPInstanceConfig that) {
        return builder.append(publishers, that.publishers)
            .append(continueOnError, that.continueOnError)
            .append(failOnError, that.failOnError)
            .append(alwaysPublishFromMaster, that.alwaysPublishFromMaster);
    }
    
    protected ToStringBuilder addToToString(ToStringBuilder builder) {
        return builder.append("publishers", publishers)
            .append("continueOnError", continueOnError)
            .append("failOnError", failOnError)
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
