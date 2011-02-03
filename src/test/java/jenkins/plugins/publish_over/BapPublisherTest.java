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

import jenkins.plugins.publish_over.helper.BPBuildInfoFactory;
import jenkins.plugins.publish_over.helper.BPHostConfigurationFactory;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class BapPublisherTest {
    
    private BPBuildInfo buildInfo = new BPBuildInfoFactory().createEmpty();
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private BPClient mockClient = mockControl.createMock(BPClient.class);
    private BPHostConfiguration hostConfiguration = new BPHostConfigurationFactory().create("TEST-CONFIG", mockClient);
    private List<BPTransfer> transfers = new LinkedList<BPTransfer>();

    @Test public void testTransfersExecutedAndClientNotified() throws Exception {
        BPTransfer transfer1 = createHappyTransfer(2);
        BPTransfer transfer2 = createHappyTransfer(3);
        BPTransfer transfer3 = createHappyTransfer(4);
        transfers.addAll(Arrays.asList(new BPTransfer[]{transfer1, transfer2, transfer3}));
        BapPublisher publisher = new BapPublisher(hostConfiguration.getName(), false, transfers);
        mockClient.disconnectQuietly();
        
        mockControl.replay();
        publisher.perform(hostConfiguration, buildInfo);
        mockControl.verify();
    }
    
    private BPTransfer createHappyTransfer(int numberOfFilesTransferred) throws Exception {
        BPTransfer transfer = mockControl.createMock(BPTransfer.class);
        mockClient.beginTransfers(transfer);
        expect(transfer.hasConfiguredSourceFiles()).andReturn(true);
        expect(transfer.transfer(buildInfo, mockClient)).andReturn(numberOfFilesTransferred);
        mockClient.endTransfers(transfer);
        return transfer;
    }

    @Test public void testExceptionPropagatedAndClientDisconnected() throws Exception {
        BPTransfer transfer1 = mockControl.createMock(BPTransfer.class);
        BPTransfer transfer2 = mockControl.createMock(BPTransfer.class);
        transfers.addAll(Arrays.asList(new BPTransfer[]{transfer1, transfer2}));
        BapPublisher publisher = new BapPublisher(hostConfiguration.getName(), false, transfers);
        RuntimeException toThrow = new RuntimeException("xxx");        
        mockClient.beginTransfers(transfer1);
        expect(transfer1.hasConfiguredSourceFiles()).andReturn(true);
        expect(transfer1.transfer(buildInfo, mockClient)).andThrow(toThrow);
        
        mockClient.disconnectQuietly();
        
        mockControl.replay();
        try {
            publisher.perform(hostConfiguration, buildInfo);
            fail();
        } catch (Exception t) {
            assertSame(toThrow, t);
        }
        mockControl.verify();
    }
    
    @Test public void testVerbositySetInBuildInfo() throws Exception {
        BPTransfer transferQuiet = new BPTransfer("", "", "", false, false) {
            public int transfer(BPBuildInfo buildInfo, BPClient client) throws Exception {
                assertFalse(buildInfo.isVerbose());
                return 5;
            }
        };
        BPTransfer transferVerbose = new BPTransfer("", "", "", false, false) {
            public int transfer(BPBuildInfo buildInfo, BPClient client) throws Exception {
                assertTrue(buildInfo.isVerbose());
                return 5;
            }
        };
        BPHostConfiguration hostConfig = new BPHostConfigurationFactory().create("TEST-CONFIG", mock(BPClient.class));
        
        BapPublisher publisher = new BapPublisher(hostConfig.getName(), false, Arrays.asList(new BPTransfer[]{transferQuiet, transferQuiet}));
        publisher.perform(hostConfig, buildInfo);
        publisher = new BapPublisher(hostConfig.getName(), true, Arrays.asList(new BPTransfer[]{transferVerbose, transferVerbose}));
        publisher.perform(hostConfig, buildInfo);
    }
    
}
