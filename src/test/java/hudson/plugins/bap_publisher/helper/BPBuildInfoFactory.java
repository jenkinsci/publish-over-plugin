package hudson.plugins.bap_publisher.helper;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.bap_publisher.BPBuildInfo;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedHashMap;

public class BPBuildInfoFactory {
    
    public BPBuildInfo createEmpty() {
        return new BPBuildInfo(new LinkedHashMap<String, String>(), new FilePath(new File("")), Calendar.getInstance(), TaskListener.NULL, "");
    }
    
}
