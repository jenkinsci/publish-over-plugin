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
        BPTransfer transfer1 = mockControl.createMock(BPTransfer.class);
        BPTransfer transfer2 = mockControl.createMock(BPTransfer.class);
        BPTransfer transfer3 = mockControl.createMock(BPTransfer.class);
        transfers.addAll(Arrays.asList(new BPTransfer[]{transfer1, transfer2, transfer3}));
        BapPublisher publisher = new BapPublisher(hostConfiguration.getName(), false, transfers);
        
        mockClient.beginTransfers(transfer1);
        expect(transfer1.transfer(buildInfo, mockClient)).andReturn(2);
        mockClient.endTransfers(transfer1);
        
        mockClient.beginTransfers(transfer2);
        expect(transfer2.transfer(buildInfo, mockClient)).andReturn(3);
        mockClient.endTransfers(transfer2);
        
        mockClient.beginTransfers(transfer3);
        expect(transfer3.transfer(buildInfo, mockClient)).andReturn(4);
        mockClient.endTransfers(transfer3);
        
        mockClient.disconnectQuietly();
        
        mockControl.replay();
        publisher.perform(hostConfiguration, buildInfo);
        mockControl.verify();
    }
    
    @Test public void testExceptionPropagatedAndClientDisconnected() throws Exception {
        BPTransfer transfer1 = mockControl.createMock(BPTransfer.class);
        BPTransfer transfer2 = mockControl.createMock(BPTransfer.class);
        transfers.addAll(Arrays.asList(new BPTransfer[]{transfer1, transfer2}));
        BapPublisher publisher = new BapPublisher(hostConfiguration.getName(), false, transfers);
        RuntimeException toThrow = new RuntimeException("xxx");        
        
        mockClient.beginTransfers(transfer1);
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
