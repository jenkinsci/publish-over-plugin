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

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

public class BPPluginDescriptor<HOST_CONFIG extends BPHostConfiguration, COMMON_CONFIG> 
            extends BuildStepDescriptor<Publisher> {

    private final transient Log log = LogFactory.getLog(BPPluginDescriptor.class);
    private BPDescriptorMessages msg;
    private Class<COMMON_CONFIG> commonConfigClass;
    private Class<HOST_CONFIG> hostConfigClass;
    private COMMON_CONFIG commonConfig;
    private CopyOnWriteList<HOST_CONFIG> hostConfigurations = new CopyOnWriteList<HOST_CONFIG>();

    public BPPluginDescriptor(BPDescriptorMessages messages, Class pluginClass, Class<HOST_CONFIG> hostConfigClass, 
                              Class<COMMON_CONFIG> commonConfigClass) {
        super(pluginClass);
        load();
        this.hostConfigClass = hostConfigClass;
        this.commonConfigClass = commonConfigClass;
        this.msg = messages;
    }
    
    public COMMON_CONFIG getCommonConfig() { return commonConfig; }
    public void setCommonConfig(COMMON_CONFIG commonConfig) { this.commonConfig = commonConfig; }

    public String getDisplayName() {
        return msg.displayName();
    }

    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    public List<HOST_CONFIG> getHostConfigurations() {
        return hostConfigurations.getView();
    }
    
    public HOST_CONFIG getConfiguration(String name) {
		for (HOST_CONFIG configuration : hostConfigurations) {
			if (configuration.getName().equals(name)) {
				return configuration;
			}
		}
		return null;
	}

    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        if (log.isDebugEnabled())
            log.debug(Messages.log_newInstance(formData.toString(2)));
        return super.newInstance(req, formData);
    }

    public boolean configure(StaplerRequest request, JSONObject formData) {
        if (log.isDebugEnabled())
            log.debug(Messages.log_configureGlobal(formData.toString(2)));
        List<HOST_CONFIG> newConfigurations = request.bindJSONToList(hostConfigClass, formData.get("hostconfig"));
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

    public FormValidation doTestConnection(StaplerRequest request, StaplerResponse response) {
        HOST_CONFIG hostConfig = request.bindParameters(hostConfigClass, "bap-pub.");
        if (commonConfigClass != null) {
            COMMON_CONFIG commonConfig = request.bindParameters(commonConfigClass, "common.");
            hostConfig.setCommonConfig(commonConfig);
        }
        BPBuildInfo buildInfo = createDummyBuildInfo();
        try {
            hostConfig.createClient(buildInfo).disconnect();
            return FormValidation.ok(msg.connectionOK());
        } catch (Exception e) {
            return FormValidation.errorWithMarkup("<p>"
                    + msg.connectionErr() + "</p><p>"
                    + Util.escape(hostConfig.toString()) + "</p><pre>"
                    + Util.escape(e.getClass().getCanonicalName() + ": " + e.getLocalizedMessage())
                    + "</pre>");
        }
    }

    private BPBuildInfo createDummyBuildInfo() {
        return new BPBuildInfo(
            new TreeMap<String, String>(),
            new FilePath(new File("")),
            Calendar.getInstance(),
            TaskListener.NULL,
            "",
            Hudson.getInstance().getRootPath()
        );
    }

    public static interface BPDescriptorMessages {
        String displayName();
        String connectionOK();
        String connectionErr();
    }

}
