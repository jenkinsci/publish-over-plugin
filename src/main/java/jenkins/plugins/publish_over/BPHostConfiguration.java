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

import hudson.Util;
import hudson.model.Hudson;
import hudson.security.ACL;
import hudson.util.Secret;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

@SuppressWarnings("PMD.TooManyMethods")
public abstract class BPHostConfiguration<CLIENT extends BPClient, COMMON_CONFIG> implements Serializable {

    private static final long serialVersionUID = 1L;

    @NonNull
    private String id;
    private String name;
    private String hostname;
    private String credentialId;
    private String remoteRootDir;
    private int port;
    private COMMON_CONFIG commonConfig;
    
    public BPHostConfiguration() { }

    public BPHostConfiguration(final String id, final String name, final String hostname, final String credentialId, final String remoteRootDir, final int port) {
        super();
        this.id = checkAndSetId(id);
        this.name = name;
        this.hostname = hostname;
        this.credentialId = credentialId;
        this.remoteRootDir = remoteRootDir;
        this.port = port;
    }

    @NonNull
    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }

    public String getHostname() { return hostname; }
    public void setHostname(final String hostname) { this.hostname = hostname; }
    
    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    // current bug in Jenkins (prototype/json-lib/stapler) will leave quotes around strings that start and end with brackets or braces
    // this method will allow a "hack" to add a space at the end of a String, which will be stripped before use - the std. accessor
    // is kept to ensure that the original String can persist through re-configurations.
    // this is an awful hack to enable an IPv6 address to be used as a hostname
    public String getHostnameTrimmed() {
        return Util.fixEmptyAndTrim(hostname);
    }

    public String getRemoteRootDir() { return remoteRootDir; }
    public void setRemoteRootDir(final String remoteRootDir) { this.remoteRootDir = remoteRootDir; }

    public int getPort() { return port; }
    public void setPort(final int port) { this.port = port; }

    public COMMON_CONFIG getCommonConfig() { return commonConfig; }
    public void setCommonConfig(final COMMON_CONFIG commonConfig) { this.commonConfig = commonConfig; }

    public CLIENT createClient(final BPBuildInfo buildInfo, final BapPublisher publisher) {
        return createClient(buildInfo);
    }

    public abstract CLIENT createClient(BPBuildInfo buildInfo);

    protected String getUsername() {
        String username = null;
        UsernamePasswordCredentialsImpl credential = getCredential();
        if (credential != null) {
            username = credential.getUsername();
        }
        return username;
    }

    protected String getPassword() {
        String password = null;
        UsernamePasswordCredentialsImpl credential = getCredential();
        if (credential != null) {
            password = Secret.toString(credential.getPassword());
        }
        return password;
    }

    protected String getEncryptedPassword() {
        String encryptedPassword = null;
        UsernamePasswordCredentialsImpl credential = getCredential();
        if (credential != null) {
            encryptedPassword = credential.getPassword().getEncryptedValue();
        }
        return encryptedPassword;
    }

    protected UsernamePasswordCredentialsImpl getCredential() {
        for (UsernamePasswordCredentialsImpl credential : CredentialsProvider.lookupCredentials(UsernamePasswordCredentialsImpl.class,
            Hudson.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement> emptyList())) {
            if (credential.getId().equals(credentialId)) {
                return credential;
            }
        }
        return null;
    }

    protected boolean isDirectoryAbsolute(final String directory) {
        if ((directory == null) || (directory.length() < 1))
            return false;
        final char first = directory.charAt(0);
        return (first == '/') || (first == '\\');
    }

    protected void changeToRootDirectory(final BPClient client) throws IOException {
        final String remoteRootDir = getRemoteRootDir();
        if ((Util.fixEmptyAndTrim(remoteRootDir) != null) && (!client.changeDirectory(remoteRootDir))) {
                exception(client, Messages.exception_cwdRemoteRoot(remoteRootDir));
        }
    }

    protected void exception(final BPClient client, final String message) {
        BapPublisherException.exception(client, message);
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(id)
            .append(name)
            .append(hostname)
            .append(credentialId)
            .append(remoteRootDir)
            .append(commonConfig)
            .append(port);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final BPHostConfiguration that) {
        return builder.append(id, that.id)
            .append(name, that.name)
            .append(hostname, that.hostname)
            .append(credentialId, that.credentialId)
            .append(remoteRootDir, that.remoteRootDir)
            .append(commonConfig, that.commonConfig)
            .append(port, that.port);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("id", id)
             .append("name", name)
            .append("hostname", hostname)
            .append("credentialId", credentialId)
            .append("remoteRootDir", remoteRootDir)
            .append("commonConfig", commonConfig)
            .append("port", port);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (BPHostConfiguration) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public Object readResolve() {
        return this;
    }
    
    /**
     * Returns either the id or a generated new id if the supplied id is missing.
     *
     * @param id the supplied id.
     * @return either the id or a generated new id if the supplied id is missing.
     */
    @NonNull
    private String checkAndSetId(@CheckForNull String id) {
        return StringUtils.isEmpty(id) ? UUID.randomUUID().toString() : id;
    }

}
