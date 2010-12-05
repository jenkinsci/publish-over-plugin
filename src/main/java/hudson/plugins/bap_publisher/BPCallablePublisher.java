package hudson.plugins.bap_publisher;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class BPCallablePublisher implements FilePath.FileCallable<Void> {

    private static Log LOG = LogFactory.getLog(BPCallablePublisher.class);

    static final long serialVersionUID = 1L;
    
    private BapPublisher publisher;
    private BPHostConfiguration hostConfig;
    private BPBuildInfo buildInfo;

    public BPCallablePublisher() {}

    public BPCallablePublisher(BapPublisher publisher, BPHostConfiguration hostConfig, BPBuildInfo buildInfo) {
        this.publisher = publisher;
        this.hostConfig = hostConfig;
        this.buildInfo = buildInfo;
    }

    public BapPublisher getPublisher() { return publisher; }
    public void setPublisher(BapPublisher publisher) { this.publisher = publisher; }

    public BPBuildInfo getBuildInfo() { return buildInfo; }
    public void setBuildInfo(BPBuildInfo buildInfo) { this.buildInfo = buildInfo; }

    public Void invoke(File f, VirtualChannel channel) throws IOException {
        try {
            printHostName();
            publisher.perform(hostConfig, buildInfo);
        } catch (Exception e) {
            throw new BapPublisherException(Messages.exception_remoteCallException(e.getLocalizedMessage()), e);
        }
        return null;
    }

    private void printHostName() {
        try {
            buildInfo.println(Messages.console_publishFromHost_message(InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException uhe) {
            LOG.warn(Messages.exception_failedToGetHostName(), uhe);
            buildInfo.println(Messages.console_publishFromHost_unknown(uhe.getLocalizedMessage()));
        }
    }
}
