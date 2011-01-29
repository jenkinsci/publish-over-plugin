package jenkins.plugins.publish_over;

public interface BPHostConfigurationAccess<CLIENT extends BPClient> {
    BPHostConfiguration<CLIENT> getConfiguration(String name);
}
