package hudson.plugins.bap_publisher;

public class BapPublisherException extends RuntimeException {

    public static void exception(BPClient client, String message) {
        exception(client, message, null);
    }

    public static void exception(BPClient client, String message, Throwable throwable) {
        client.disconnectQuietly();
        if (throwable != null)
            throw new BapPublisherException(message, throwable);
        else
            throw new BapPublisherException(message);
    }

    public BapPublisherException(String message) {
        super(message);
    }

    public BapPublisherException(String message, Throwable t) {
        super(message, t);
    }

}
