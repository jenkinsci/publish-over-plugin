package hudson.plugins.bap_publisher;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;

public abstract class BPHostConfiguration<CLIENT extends BPClient> implements Serializable {

    private String name;
	private String hostname;
    private String username;
    private String password;
    private String remoteRootDir;
    private int port;

	public BPHostConfiguration() {}

	public BPHostConfiguration(String name, String hostname, String username, String password, String remoteRootDir, int port) {
		this.name = name;
		this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.remoteRootDir = remoteRootDir;
        this.port = port;
	}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRemoteRootDir() { return remoteRootDir; }
    public void setRemoteRootDir(String remoteRootDir) { this.remoteRootDir = remoteRootDir; }

	public int getPort() { return port; }
	public void setPort(int port) { this.port = port; }

    public abstract CLIENT createClient(BPBuildInfo buildInfo) throws BapPublisherException;

    protected boolean isDirectoryAbsolute(String directory) {
        if (directory == null)
            return false;
        return directory.startsWith("/") || directory.startsWith("\\");
    }
    
    protected HashCodeBuilder createHashCodeBuilder() {
        return addToHashCode(new HashCodeBuilder());
    }

    protected HashCodeBuilder addToHashCode(HashCodeBuilder builder) {
        return builder.append(name)
            .append(hostname)
            .append(username)
            .append(password)
            .append(remoteRootDir)
            .append(port);
    }
    
    protected EqualsBuilder createEqualsBuilder(BPHostConfiguration that) {
        return addToEquals(new EqualsBuilder(), that);
    }
    
    protected EqualsBuilder addToEquals(EqualsBuilder builder, BPHostConfiguration that) {
        return builder.append(name, that.name)
            .append(hostname, that.hostname)
            .append(username, that.username)
            .append(password, that.password)
            .append(remoteRootDir, that.remoteRootDir)
            .append(port, that.port);
    }
    
    protected ToStringBuilder addToToString(ToStringBuilder builder) {
        return builder.append("name", name)
            .append("hostname", hostname)
            .append("username", username)
            .append("password", "***")
            .append("remoteRootDir", remoteRootDir)
            .append("port", port);
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        return createEqualsBuilder((BPHostConfiguration) o).isEquals();
    }

    public int hashCode() {
        return createHashCodeBuilder().toHashCode();
    }
    
    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

}
