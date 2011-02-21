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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BapPublisher<TRANSFER extends BPTransfer> implements Serializable {

    static final long serialVersionUID = 1L;

    private String configName;
    private boolean verbose;
    private List<TRANSFER> transfers;
    private boolean useWorkspaceInPromotion;
    private boolean usePromotionTimestamp;

    public BapPublisher() { }

    public BapPublisher(final String configName, final boolean verbose, final List<TRANSFER> transfers) {
        this(configName, verbose, transfers, false, false);
    }

    public BapPublisher(final String configName, final boolean verbose, final List<TRANSFER> transfers,
                        final boolean useWorkspaceInPromotion, final boolean usePromotionTimestamp) {
        this.configName = configName;
        this.verbose = verbose;
        setTransfers(transfers);
        this.useWorkspaceInPromotion = useWorkspaceInPromotion;
        this.usePromotionTimestamp = usePromotionTimestamp;
    }

    public String getConfigName() {
        return configName;
    }
    public void setConfigName(final String configName) {
        this.configName = configName;
    }

    public boolean isUseWorkspaceInPromotion() {
        return useWorkspaceInPromotion;
    }
    public void setUseWorkspaceInPromotion(final boolean useWorkspaceInPromotion) {
        this.useWorkspaceInPromotion = useWorkspaceInPromotion;
    }

    public boolean isUsePromotionTimestamp() {
        return usePromotionTimestamp;
    }
    public void setUsePromotionTimestamp(final boolean usePromotionTimestamp) {
        this.usePromotionTimestamp = usePromotionTimestamp;
    }

    public boolean isVerbose() {
        return verbose;
    }
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public List<TRANSFER> getTransfers() {
        return transfers;
    }
    public void setTransfers(final List<TRANSFER> transfers) {
        if (transfers == null) {
            this.transfers = new ArrayList<TRANSFER>();
        } else {
            this.transfers = transfers;
        }
    }

    private int sumTransfers(final List<Integer> transferred) {
        int total = 0;
        for (int tx : transferred) {
            total += tx;
        }
        return total;
    }

    private void printNumberOfFilesTransferred(final BPBuildInfo buildInfo, final List<Integer> transferred) {
        int total = sumTransfers(transferred);
        String countString = "" + total;
        if (transferred.size() > 1) {
            countString = total + " ( " + StringUtils.join(transferred, " + ") + " )";
        }
        buildInfo.println(Messages.console_transferredXFiles(countString));
    }

    public void setEffectiveEnvironmentInBuildInfo(final BPBuildInfo buildInfo) {
        buildInfo.setVerbose(verbose);
        BPBuildEnv current = buildInfo.getCurrentBuildEnv();
        BPBuildEnv target = buildInfo.getTargetBuildEnv();
        if (target == null) {
            buildInfo.setEnvVars(current.getEnvVars());
            buildInfo.setBaseDirectory(current.getBaseDirectory());
            buildInfo.setBuildTime(current.getBuildTime());
        } else {
            buildInfo.setBaseDirectory(useWorkspaceInPromotion ? current.getBaseDirectory() : target.getBaseDirectory());
            buildInfo.setBuildTime(usePromotionTimestamp ? current.getBuildTime() : target.getBuildTime());
            Map<String, String> effectiveEnvVars = current.getEnvVarsWithPrefix(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX);
            effectiveEnvVars.putAll(target.getEnvVars());
            buildInfo.setEnvVars(effectiveEnvVars);
        }
    }

    public void perform(final BPHostConfiguration hostConfig, final BPBuildInfo buildInfo) throws Exception {
        buildInfo.println(Messages.console_connecting(configName));
        BPClient client = hostConfig.createClient(buildInfo);
        List<Integer> transferred = new ArrayList<Integer>();
        try {
            for (TRANSFER transfer : transfers) {
                client.beginTransfers(transfer);
                if (transfer.hasConfiguredSourceFiles())
                    transferred.add(transfer.transfer(buildInfo, client));
                else
                    transferred.add(0);
                client.endTransfers(transfer);
            }
            printNumberOfFilesTransferred(buildInfo, transferred);
        } finally {
            buildInfo.println(Messages.console_disconnecting(configName));
            client.disconnectQuietly();
        }
    }

    protected HashCodeBuilder createHashCodeBuilder() {
        return addToHashCode(new HashCodeBuilder());
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(configName).append(verbose).append(transfers)
            .append(useWorkspaceInPromotion).append(usePromotionTimestamp);
    }

    protected EqualsBuilder createEqualsBuilder(final BapPublisher that) {
        return addToEquals(new EqualsBuilder(), that);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapPublisher that) {
        return builder.append(configName, that.configName)
            .append(verbose, that.verbose)
            .append(transfers, that.transfers)
            .append(useWorkspaceInPromotion, that.useWorkspaceInPromotion)
            .append(usePromotionTimestamp, that.usePromotionTimestamp);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("configName", configName)
            .append("verbose", verbose)
            .append("transfers", transfers)
            .append("useWorkspaceInPromotion", useWorkspaceInPromotion)
            .append("usePromotionTimestamp", usePromotionTimestamp);
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return createEqualsBuilder((BapPublisher) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
