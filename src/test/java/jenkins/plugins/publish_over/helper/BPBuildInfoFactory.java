package jenkins.plugins.publish_over.helper;

import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.plugins.publish_over.BPBuildInfo;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedHashMap;

public class BPBuildInfoFactory {
    
    public BPBuildInfo createEmpty() {
        return new BPBuildInfo(new LinkedHashMap<String, String>(), new FilePath(new File("")), Calendar.getInstance(), TaskListener.NULL, "");
    }
    
}
