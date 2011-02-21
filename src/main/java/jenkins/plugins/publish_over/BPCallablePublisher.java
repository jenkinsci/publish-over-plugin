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
import hudson.remoting.VirtualChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class BPCallablePublisher implements FilePath.FileCallable<Void> {

    static final long serialVersionUID = 1L;

    private static final transient Log LOG = LogFactory.getLog(BPCallablePublisher.class);

    private BapPublisher publisher;
    private BPHostConfiguration hostConfig;
    private BPBuildInfo buildInfo;

    public BPCallablePublisher() { }

    public BPCallablePublisher(final BapPublisher publisher, final BPHostConfiguration hostConfig, final BPBuildInfo buildInfo) {
        this.publisher = publisher;
        this.hostConfig = hostConfig;
        this.buildInfo = buildInfo;
    }

    public BapPublisher getPublisher() { return publisher; }
    public void setPublisher(final BapPublisher publisher) { this.publisher = publisher; }

    public BPBuildInfo getBuildInfo() { return buildInfo; }
    public void setBuildInfo(final BPBuildInfo buildInfo) { this.buildInfo = buildInfo; }

    public Void invoke(final File f, final VirtualChannel channel) throws IOException {
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
