package jenkins.plugins.publish_over;

public abstract class BPDefaultClient<TRANSFER extends BPTransfer> implements BPClient<TRANSFER> {

    public void beginTransfers(TRANSFER transfer) { }

    public void endTransfers(TRANSFER transfer) { }

}
