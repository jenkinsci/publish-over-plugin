package hudson.plugins.bap_publisher;

import hudson.FilePath;

import java.io.IOException;
import java.io.InputStream;

public interface BPClient<TRANSFER extends BPTransfer> {

    boolean changeToInitialDirectory();

    boolean changeDirectory(String directory);

    boolean makeDirectory(String directory);

    void beginTransfers(TRANSFER transfer);

    void transferFile(TRANSFER transfer, FilePath filePath, InputStream fileContent) throws Exception;

    void endTransfers(TRANSFER transfer);

    void disconnect() throws Exception;

    void disconnectQuietly();

}
