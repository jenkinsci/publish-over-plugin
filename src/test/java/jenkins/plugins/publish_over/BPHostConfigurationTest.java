package jenkins.plugins.publish_over;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class BPHostConfigurationTest {
    
    private BPHostConfiguration hostConfig;

    @Before public void setUp() {
        hostConfig = new ConcreteBPHostConfiguration();
    }
    
    @Test public void testIsAbsolute_falseForNull() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute(null));
    }
    
    @Test public void testIsAbsolute_falseForEmpty() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute(""));
    }
    
    @Test public void testIsAbsolute_falseForRelativeWin() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute("some\\file\\path"));
    }
    
    @Test public void testIsAbsolute_falseForRelativeUnix() throws Exception {
        assertFalse(hostConfig.isDirectoryAbsolute("some/file/path"));
    }
    
    @Test public void testIsAbsolute_trueForWin() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("\\some\\file\\path"));
    }
    
    @Test public void testIsAbsolute_trueForUnix() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("/some/file/path"));
    }
    
    @Test public void testIsAbsolute_trueForWinRoot() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("\\"));
    }
    
    @Test public void testIsAbsolute_trueForUnixRoot() throws Exception {
        assertTrue(hostConfig.isDirectoryAbsolute("/"));
    }
        
    private static class ConcreteBPHostConfiguration extends BPHostConfiguration {

        @Override
        public BPClient createClient(BPBuildInfo buildInfo) throws BapPublisherException {
            return null;
        }
        
    }
    
}
