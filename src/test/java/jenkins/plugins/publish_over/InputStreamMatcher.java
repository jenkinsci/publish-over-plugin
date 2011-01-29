package jenkins.plugins.publish_over;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class InputStreamMatcher implements IArgumentMatcher {

    public static final Log LOG = LogFactory.getLog(InputStreamMatcher.class);

    public static InputStream streamContains(byte[] expectedContents) {
        EasyMock.reportMatcher(new InputStreamMatcher(expectedContents));
        return null;
    }

    private byte[] expectedContents;

    public InputStreamMatcher(byte[] expectedContents) {
        this.expectedContents = expectedContents;
    }

    public boolean matches(Object o) {
        if (!(o instanceof InputStream))
            return false;

        try {
            byte[] actual = IOUtils.toByteArray((InputStream)o);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Expected (md5) = " + DigestUtils.md5Hex(expectedContents));
                LOG.debug("Actual   (md5) = " + DigestUtils.md5Hex(actual));
            }
            return Arrays.equals(expectedContents, actual);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to read contents of InputStream", ioe);
        }
    }

    public void appendTo(StringBuffer stringBuffer) {
        stringBuffer.append("Expected InputStream with contents (md5) = " + DigestUtils.md5Hex(expectedContents));
    }
}
