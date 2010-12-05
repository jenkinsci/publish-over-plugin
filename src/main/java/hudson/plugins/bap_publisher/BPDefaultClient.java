package hudson.plugins.bap_publisher;

public abstract class BPDefaultClient<TRANSFER extends BPTransfer> implements BPClient<TRANSFER> {

    public void beginTransfers(TRANSFER transfer) { }

    public void endTransfers(TRANSFER transfer) { }

}
