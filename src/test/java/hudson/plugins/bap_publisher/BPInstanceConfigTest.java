package hudson.plugins.bap_publisher;

import hudson.model.Result;
import hudson.plugins.bap_publisher.helper.BPBuildInfoFactory;
import hudson.plugins.bap_publisher.helper.BPHostConfigurationFactory;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class BPInstanceConfigTest {

    private static Level originalLogLevel;
    
    @BeforeClass public static void before() {
        String packageName = getLoggerName();
        originalLogLevel = Logger.getLogger(packageName).getLevel();
        Logger.getLogger(packageName).setLevel(Level.OFF);
    }

    @AfterClass public static void after() {
        Logger.getLogger(getLoggerName()).setLevel(originalLogLevel);
    }

    private static String getLoggerName() {
        return BPInstanceConfig.class.getCanonicalName();
    }

    private BPBuildInfo buildInfo = new BPBuildInfoFactory().createEmpty();
    private IMocksControl mockControl = EasyMock.createStrictControl();
    private BPHostConfigurationAccess mockHostConfigurationAccess = mock(BPHostConfigurationAccess.class);
    private BPHostConfiguration hostConfiguration = new BPHostConfigurationFactory().create("TEST-CONFIG");
    private List<BapPublisher> publishers = new LinkedList<BapPublisher>();

    @Before public void setUp() throws Exception {
        when(mockHostConfigurationAccess.getConfiguration(Mockito.anyString())).thenReturn(hostConfiguration);
    }

    @Test public void testPerformReturnsSuccessIfNoExceptionsThrown() throws Exception {
        BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false);
        instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
        BapPublisher mockPublisher = createAndAddMockPublisher(hostConfiguration.getName());
        mockPublisher.perform(hostConfiguration, buildInfo);
        
        assertResult(Result.SUCCESS, instanceConfig);
    }

    @Test public void testPerformReturnsUnstableAndDoesNotInvokeOtherPublishers() throws Exception {
        BapPublisher mockPub1 = createAndAddMockPublisher(hostConfiguration.getName());
        mockPub1.perform(hostConfiguration, buildInfo);
        expectLastCall().andThrow(new RuntimeException("Bad stuff here!"));
        createAndAddMockPublisher(null);
        
        BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false);
        instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
        
        assertResult(Result.UNSTABLE, instanceConfig);
    }
    
    @Test public void testPerformReturnsUnstableAndInvokesOtherPublishersWhenContinueOnErrorSet() throws Exception {
        BapPublisher mockPub1 = createAndAddMockPublisher(hostConfiguration.getName());
        mockPub1.perform(hostConfiguration, buildInfo);
        expectLastCall().andThrow(new RuntimeException("Bad stuff here!"));
        BapPublisher mockPub2 = createAndAddMockPublisher(hostConfiguration.getName());
        mockPub2.perform(hostConfiguration, buildInfo);
        
        BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, true, false, false);
        instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
        
        assertResult(Result.UNSTABLE, instanceConfig);
    }
    
    @Test public void testPerformReturnsUnstableWhenNoHostConfigFound() throws Exception {
        BapPublisher mockPublisher = createAndAddMockPublisher(null);
        expect(mockPublisher.getConfigName()).andReturn(hostConfiguration.getName());
       
        reset(mockHostConfigurationAccess);
        when(mockHostConfigurationAccess.getConfiguration(hostConfiguration.getName())).thenReturn(null);
        
        BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, false, false);
        instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
        
        assertResult(Result.UNSTABLE, instanceConfig);
        verify(mockHostConfigurationAccess).getConfiguration(hostConfiguration.getName());
    }
    
    @Test public void testPerformReturnsFailureIfFailOnError() throws Exception {
        BapPublisher mockPub1 = createAndAddMockPublisher(hostConfiguration.getName());
        mockPub1.perform(hostConfiguration, buildInfo);
        expectLastCall().andThrow(new RuntimeException("Bad stuff here!"));
        createAndAddMockPublisher(null);
        
        BPInstanceConfig instanceConfig = new BPInstanceConfig(publishers, false, true, false);
        instanceConfig.setHostConfigurationAccess(mockHostConfigurationAccess);
        
        assertResult(Result.FAILURE, instanceConfig);
    }

    private BapPublisher createAndAddMockPublisher(String hostConfigurationName) {
        BapPublisher mockPublisher = mockControl.createMock(BapPublisher.class);
        if (hostConfigurationName != null)
            expect(mockPublisher.getConfigName()).andReturn(hostConfigurationName);
        publishers.add(mockPublisher);
        return mockPublisher;
    }
    
    private void assertResult(Result expectedResult, BPInstanceConfig instanceConfig) {
        mockControl.replay();
        assertEquals(expectedResult, instanceConfig.perform(buildInfo));
        mockControl.verify();
    }    

}
