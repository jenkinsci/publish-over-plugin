package hudson.plugins.bap_publisher;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class RandomFile {

    private static Random random = new Random();

    private File file;
    private byte[] contents;

    public RandomFile(File directory, String filename) {
        this(new File(directory, filename));
    }

    public RandomFile(File file) {
        this(file, 200);
    }

    public RandomFile(File file, int size) {
        this.file = file;

        contents = new byte[size];
        random.nextBytes(contents);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            FileUtils.writeByteArrayToFile(file, contents);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to create file [" + file.getAbsolutePath()  + "]", ioe);
        }
    }

    public File getFile() {
        return file;
    }

    public byte[] getContents() {
        return contents;
    }

    public String getFileName() {
        return file.getName();
    }

}
