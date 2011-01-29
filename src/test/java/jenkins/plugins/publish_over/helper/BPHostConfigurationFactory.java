package jenkins.plugins.publish_over.helper;

import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPClient;
import jenkins.plugins.publish_over.BPHostConfiguration;
import jenkins.plugins.publish_over.BapPublisherException;

import static org.mockito.Mockito.mock;

public class BPHostConfigurationFactory {
    
    public BPHostConfiguration create(String configName) {
        BPHostConfiguration config = new ConcreteBPHostConfiguration();
        config.setName(configName);
        return config;
    }
    
    public BPHostConfiguration create(String configName, BPClient client) {
        BPHostConfiguration config = new ConcreteBPHostConfiguration(client);
        config.setName(configName);
        return config;
    }
    
    public static class ConcreteBPHostConfiguration<CLIENT extends BPClient> extends BPHostConfiguration<CLIENT> {
        
        private CLIENT client;

        public ConcreteBPHostConfiguration() {
            // beautiful :-(
            this((CLIENT) mock(BPClient.class));
        }
        
        public ConcreteBPHostConfiguration(CLIENT client) {
            this.client = client;
        }
        
        @Override
        public CLIENT createClient(BPBuildInfo buildInfo) throws BapPublisherException {
            return client;
        }
    }
    
}
