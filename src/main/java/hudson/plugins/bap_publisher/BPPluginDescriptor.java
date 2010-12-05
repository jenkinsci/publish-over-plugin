package hudson.plugins.bap_publisher;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
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

public class BPPluginDescriptor<HOST_CONFIG extends BPHostConfiguration> extends BuildStepDescriptor<Publisher> {

    private final transient Log log = LogFactory.getLog(BPPluginDescriptor.class);
    private final CopyOnWriteList<HOST_CONFIG> hostConfigurations = new CopyOnWriteList<HOST_CONFIG>();
    private final Class<HOST_CONFIG> hostConfigClass;
    private final DescriptorMessages msg;

    public BPPluginDescriptor(DescriptorMessages messages, Class pluginClass, Class<HOST_CONFIG> hostConfigClass) {
        super(pluginClass);
        load();
        this.hostConfigClass = hostConfigClass;
        this.msg = messages;
    }

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

    public boolean configure(StaplerRequest req, JSONObject formData) {
        if (log.isDebugEnabled())
            log.debug(Messages.log_configureGlobal(formData.toString(2)));
        hostConfigurations.replaceBy(req.bindJSONToList(hostConfigClass, formData.get("hostconfig")));
        save();
        return true;
    }

    public FormValidation doTestConnection(StaplerRequest request, StaplerResponse response) {
        HOST_CONFIG hostConfig = request.bindParameters(hostConfigClass, "bap-pub.");
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
            ""
        );
    }

    public static interface DescriptorMessages {

        String displayName();
        String connectionOK();
        String connectionErr();

    }

}
