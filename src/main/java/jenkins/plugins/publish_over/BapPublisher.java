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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

// serializable + only actually 4 "real" methods in here all rest accessors and boiler str/has/eq
@SuppressWarnings({ "PMD.LooseCoupling", "PMD.TooManyMethods" })
public class BapPublisher<TRANSFER extends BPTransfer> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(BapPublisher.class.getName());

    private String configId;
    private String configName;
    private boolean verbose;
    private ArrayList<TRANSFER> transfers;
    private boolean useWorkspaceInPromotion;
    private boolean usePromotionTimestamp;
    private Retry retry;
    private PublisherLabel label;
    private Credentials credentials;

    public BapPublisher() { }

    public BapPublisher(final String configId, final String configName, final boolean verbose, final ArrayList<TRANSFER> transfers,
                        final boolean useWorkspaceInPromotion, final boolean usePromotionTimestamp, final Retry retry,
                        final PublisherLabel label, final Credentials credentials) {
        this.configId = configId;
        this.configName = configName;
        this.verbose = verbose;
        setTransfers(transfers);
        this.useWorkspaceInPromotion = useWorkspaceInPromotion;
        this.usePromotionTimestamp = usePromotionTimestamp;
        this.retry = retry;
        this.label = label;
        this.credentials = credentials;
        System.out.println("BapPublisher: " + toString());
    }

    public String getConfigId() {
        return configName;
    }
    public String getConfigName() {
        return configName;
    }
    public final boolean isUseWorkspaceInPromotion() {
        return useWorkspaceInPromotion;
    }
    public final void setUseWorkspaceInPromotion(final boolean useWorkspaceInPromotion) {
        this.useWorkspaceInPromotion = useWorkspaceInPromotion;
    }

    public final boolean isUsePromotionTimestamp() {
        return usePromotionTimestamp;
    }
    public final void setUsePromotionTimestamp(final boolean usePromotionTimestamp) {
        this.usePromotionTimestamp = usePromotionTimestamp;
    }

    public final boolean isVerbose() {
        return verbose;
    }
    public final void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public final ArrayList<TRANSFER> getTransfers() {
        return transfers;
    }
    public final void setTransfers(final ArrayList<TRANSFER> transfers) {
        if (transfers == null) {
            this.transfers = new ArrayList<TRANSFER>();
        } else {
            this.transfers = transfers;
        }
    }

    public Retry getRetry() {
        return retry;
    }

    public PublisherLabel getLabel() {
        return label;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    private int sumTransfers(final List<Integer> transferred) {
        int total = 0;
        for (int tx : transferred) {
            total += tx;
        }
        return total;
    }

    private void printNumberOfFilesTransferred(final BPBuildInfo buildInfo, final List<Integer> transferred) {
        final int total = sumTransfers(transferred);
        String countString = Integer.toString(total);
        if (transferred.size() > 1) {
            countString = total + " ( " + StringUtils.join(transferred, " + ") + " )";
        }
        buildInfo.println(Messages.console_transferredXFiles(countString));
    }

    public void setEffectiveEnvironmentInBuildInfo(final BPBuildInfo buildInfo) {
        buildInfo.setVerbose(verbose);
        final BPBuildEnv current = buildInfo.getCurrentBuildEnv();
        final BPBuildEnv target = buildInfo.getTargetBuildEnv();
        if (target == null) {
            buildInfo.setEnvVars(current.getEnvVars());
            buildInfo.setBaseDirectory(current.getBaseDirectory());
            buildInfo.setBuildTime(current.getBuildTime());
        } else {
            buildInfo.setBaseDirectory(useWorkspaceInPromotion ? current.getBaseDirectory() : target.getBaseDirectory());
            buildInfo.setBuildTime(usePromotionTimestamp ? current.getBuildTime() : target.getBuildTime());
            final TreeMap<String, String> effectiveEnvVars = current.getEnvVarsWithPrefix(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX);
            effectiveEnvVars.putAll(target.getEnvVars());
            buildInfo.setEnvVars(effectiveEnvVars);
        }
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public void perform(final BPHostConfiguration hostConfig, final BPBuildInfo buildInfo) throws Exception {
        final Performer performer = new Performer(hostConfig, buildInfo);
        printNumberOfFilesTransferred(buildInfo, performer.perform());
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(configId).append(configName).append(verbose).append(transfers)
            .append(useWorkspaceInPromotion).append(usePromotionTimestamp)
            .append(retry).append(label).append(credentials);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BapPublisher that) {
        return builder.append(configId, that.configId)
            .append(configName, that.configName)
            .append(verbose, that.verbose)
            .append(transfers, that.transfers)
            .append(useWorkspaceInPromotion, that.useWorkspaceInPromotion)
            .append(usePromotionTimestamp, that.usePromotionTimestamp)
            .append(retry, that.retry)
            .append(label, that.label)
            .append(credentials, that.credentials);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("configId", configId)
            .append("configName", configName)
            .append("verbose", verbose)
            .append("transfers", transfers)
            .append("useWorkspaceInPromotion", useWorkspaceInPromotion)
            .append("usePromotionTimestamp", usePromotionTimestamp)
            .append("retry", retry)
            .append("label", label)
            .append("credentials", credentials);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BapPublisher) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    @SuppressWarnings({ "PMD.SignatureDeclareThrowsException" })
    private final class Performer {

        private final BPHostConfiguration hostConfig;
        private final BPBuildInfo buildInfo;
        private BPClient client;
        private int remainingTries;
        private final ArrayList<TRANSFER> remainingTransfers = new ArrayList<TRANSFER>();
        private final List<Integer> transferred = new ArrayList<Integer>();
        private boolean transferComplete;
        private BPTransfer.TransferState transferState;
        private Exception exception;

        protected Performer(final BPHostConfiguration hostConfig, final BPBuildInfo buildInfo) {
            this.hostConfig = hostConfig;
            this.buildInfo = buildInfo;
            remainingTries = retry == null ? 0 : retry.getRetries();
            remainingTransfers.addAll(transfers);
        }

        private List<Integer> perform() throws Exception {
            do {
                try {
                    if (credentials != null) buildInfo.put(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY, credentials);
                    buildInfo.println(Messages.console_connecting(configName));
                    client = hostConfig.createClient(buildInfo, BapPublisher.this);
                    while (!remainingTransfers.isEmpty()) {
                        beginTransfers();
                        transfer();
                        endTransfers();
                    }
                    return transferred;
                } catch (BapTransferException bte) {
                    transferState = bte.getState();
                    exception = (Exception) bte.getCause();
                } catch (Exception e) {
                    exception = e;
                } finally {
                    if (credentials != null) buildInfo.remove(BPBuildInfo.OVERRIDE_CREDENTIALS_CONTEXT_KEY);
                    if (client != null) {
                        buildInfo.println(Messages.console_disconnecting(configName));
                        client.disconnectQuietly();
                    }
                }
            } while (remainingTries-- > 0 && delay());
            throw exception;
        }

        private boolean delay() {
            LOGGER.log(Level.WARNING, Messages.log_exceptionCaught_retrying(), exception);
            buildInfo.println(Messages.console_retryDelay(exception.getLocalizedMessage(), retry.getRetryDelay()));
            try {
                Thread.sleep(retry.getRetryDelay());
            } catch (InterruptedException ie) {
                throw new BapPublisherException(Messages.exception_retryDelayInterrupted(), ie);
            }
            return true;
        }

        private void beginTransfers() {
            client.beginTransfers(remainingTransfers.get(0));
        }

        private void transfer() throws Exception {
            if (transferComplete) return;
            final BPTransfer transfer = remainingTransfers.get(0);
            if (!transfer.hasConfiguredSourceFiles()) {
                transferred.add(0);
                transferComplete = true;
                return;
            }
            if (transferState == null)
                transferred.add(transfer.transfer(buildInfo, client));
            else
                transferred.add(transfer.transfer(buildInfo, client, transferState));
            transferComplete = true;
        }

        private void endTransfers() {
            client.endTransfers(remainingTransfers.get(0));
            remainingTransfers.remove(0);
            transferState = null;
            transferComplete = false;
        }

    }

}
