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
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.List;

@SuppressWarnings("PMD.TooManyMethods")
public class BPPluginDescriptor<HOST_CONFIG extends BPHostConfiguration, COMMON_CONFIG>
            extends BuildStepDescriptor<Publisher> {

    private final BPDescriptorMessages msg;
    private final Class<COMMON_CONFIG> commonConfigClass;
    private final Class<HOST_CONFIG> hostConfigClass;
    private final CopyOnWriteList<HOST_CONFIG> hostConfigurations = new CopyOnWriteList<HOST_CONFIG>();
    private COMMON_CONFIG commonConfig;

    public BPPluginDescriptor(final BPDescriptorMessages messages, final Class pluginClass, final Class<HOST_CONFIG> hostConfigClass,
                              final Class<COMMON_CONFIG> commonConfigClass) {
        super(pluginClass);
        load();
        this.hostConfigClass = hostConfigClass;
        this.commonConfigClass = commonConfigClass;
        this.msg = messages;
    }

    public COMMON_CONFIG getCommonConfig() { return commonConfig; }
    public void setCommonConfig(final COMMON_CONFIG commonConfig) { this.commonConfig = commonConfig; }

    public String getDisplayName() {
        return msg.displayName();
    }

    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
        return true;
    }

    public List<HOST_CONFIG> getHostConfigurations() {
        return hostConfigurations.getView();
    }

    public HOST_CONFIG getConfiguration(final String name) {
        for (HOST_CONFIG configuration : hostConfigurations) {
            if (configuration.getName().equals(name)) {
                return configuration;
            }
        }
        return null;
    }

    public boolean configure(final StaplerRequest request, final JSONObject formData) {
        final List<HOST_CONFIG> newConfigurations = request.bindJSONToList(hostConfigClass, formData.get("hostconfig"));
        if (commonConfigClass != null) {
            commonConfig = request.bindJSON(commonConfigClass, formData.getJSONObject("common"));
            for (HOST_CONFIG hostConfig : newConfigurations) {
                hostConfig.setCommonConfig(commonConfig);
            }
        }
        hostConfigurations.replaceBy(newConfigurations);
        save();
        return true;
    }

    public FormValidation doCheckName(@QueryParameter final String value) {
        return BPSafeName.validateName(value);
    }
    public FormValidation doCheckHostname(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }
    public FormValidation doCheckUsername(@QueryParameter final String value) {
        return FormValidation.validateRequired(value);
    }
    public FormValidation doCheckPort(@QueryParameter final String value) {
        return FormValidation.validatePositiveInteger(value);
    }
    public FormValidation doCheckTimeout(@QueryParameter final String value) {
        return FormValidation.validateNonNegativeInteger(value);
    }
    public boolean canUseExcludes() {
        return BPTransfer.canUseExcludes();
    }

    public FormValidation doTestConnection(final StaplerRequest request, final StaplerResponse response) {
        final HOST_CONFIG hostConfig = request.bindParameters(hostConfigClass, "bap-pub.");
        if (commonConfigClass != null) {
            final COMMON_CONFIG commonConfig = request.bindParameters(commonConfigClass, "common.");
            hostConfig.setCommonConfig(commonConfig);
        }
        final BPBuildInfo buildInfo = createDummyBuildInfo();
        try {
            hostConfig.createClient(buildInfo).disconnect();
            return FormValidation.ok(msg.connectionOK());
        } catch (Exception e) {
            return FormValidation.errorWithMarkup("<p>"
                    + msg.connectionErr() + "</p><p><pre>"
                    + Util.escape(e.getClass().getCanonicalName() + ": " + e.getLocalizedMessage())
                    + "</pre></p>");
        }
    }

    private BPBuildInfo createDummyBuildInfo() {
        return new BPBuildInfo(
            TaskListener.NULL,
            "",
            Hudson.getInstance().getRootPath(),
            null,
            null
        );
    }

    public interface BPDescriptorMessages {
        String displayName();
        String connectionOK();
        String connectionErr();
    }

}
