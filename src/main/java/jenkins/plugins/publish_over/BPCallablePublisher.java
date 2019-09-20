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

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BPCallablePublisher extends MasterToSlaveFileCallable<Void> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(BPCallablePublisher.class.getName());

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

    public Void invoke(final File file, final VirtualChannel channel) throws IOException {
        try {
            printHostName();
            publisher.perform(hostConfig, buildInfo);
        } catch (Exception e) {
            final String message = Messages.exception_remoteCallException(e.getLocalizedMessage());
            LOGGER.log(Level.WARNING, message, e);
            throw new BapPublisherException(message, e);
        }
        return null;
    }

    private void printHostName() {
        try {
            buildInfo.println(Messages.console_publishFromHost_message(InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException uhe) {
            LOGGER.log(Level.WARNING, Messages.exception_failedToGetHostName(), uhe);
            buildInfo.println(Messages.console_publishFromHost_unknown(uhe.getLocalizedMessage()));
        }
    }

}
