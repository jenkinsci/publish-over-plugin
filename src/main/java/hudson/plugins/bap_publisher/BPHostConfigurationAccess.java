package hudson.plugins.bap_publisher;

public interface BPHostConfigurationAccess<CLIENT extends BPClient> {
    BPHostConfiguration<CLIENT> getConfiguration(String name);
}
