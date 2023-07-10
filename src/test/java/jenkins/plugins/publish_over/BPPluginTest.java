package jenkins.plugins.publish_over;

import hudson.FilePath;
import hudson.Functions;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import java.util.Collections;

import org.htmlunit.Page;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Future;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BPPluginTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testNormalBuild() throws Exception {

        // create project
        FreeStyleProject freeStyleProject = r.createFreeStyleProject("proj1");
        freeStyleProject.getBuildersList().add(Functions.isWindows()
                ? new BatchFile("echo Hello, world > file.txt")
                : new Shell("echo Hello, world > file.txt"));
        freeStyleProject.getPublishersList().add(new ArtifactArchiver("file.txt"));

        BapPublisher<BPTransfer> bapPublisher = new BapPublisher<>();
        bapPublisher.setTransfers(new ArrayList<>());
        bapPublisher.getTransfers().add(new BPTransfer("file.txt", null, null, null, false, false));

        DummyBPPlugin bpPlugin = new DummyBPPlugin("prefix1");
        bpPlugin.getDelegate().setPublishers(new ArrayList<>());
        bpPlugin.getDelegate().getPublishers().add(bapPublisher);
        freeStyleProject.getPublishersList().add(bpPlugin);

        // trigger normal build
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = freeStyleProject.scheduleBuild2(0);
        FreeStyleBuild freeStyleBuild = scheduleBuild2.get();

        assertTrue(freeStyleBuild.getHasArtifacts());
        FreeStyleBuild b = r.assertBuildStatusSuccess(freeStyleBuild);
    }

    @Test
    public void testPromotionBuild() throws Exception {

        // create project
        FreeStyleProject freeStyleProject = r.createFreeStyleProject("proj1");
        freeStyleProject.getBuildersList().add(Functions.isWindows()
                ? new BatchFile("echo Hello, world > file.txt")
                : new Shell("echo Hello, world > file.txt"));
        freeStyleProject.getPublishersList().add(new ArtifactArchiver("file.txt"));

        // add promotion to project
        BapPublisher<BPTransfer> bapPublisher = new BapPublisher<>();
        bapPublisher.setTransfers(new ArrayList<>());
        bapPublisher.getTransfers()
                    .add(new BPTransfer("file.txt", null, null, null, false, false));

        DummyBPPlugin bpPlugin = new DummyBPPlugin("prefix1");
        bpPlugin.getDelegate().setPublishers(new ArrayList<>());
        bpPlugin.getDelegate().getPublishers().add(bapPublisher);

        JobPropertyImpl jobProperty = new JobPropertyImpl(freeStyleProject);
        freeStyleProject.addProperty(jobProperty);
        PromotionProcess promotionProcess = jobProperty.addProcess("promo1");
        promotionProcess.getBuildSteps().add(bpPlugin);

        // trigger normal build
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = freeStyleProject.scheduleBuild2(0);
        FreeStyleBuild freeStyleBuild = scheduleBuild2.get();

        assertTrue(freeStyleBuild.getHasArtifacts());
        r.assertBuildStatusSuccess(freeStyleBuild);
        r.assertLogContains("Archiving artifacts", freeStyleBuild);

        // trigger promotion build
        assertNull(promotionProcess.getBuilds().getLastBuild());
        Future<Promotion> promotionFuture = promotionProcess.promote2(
            freeStyleBuild,
            new Cause.UserIdCause(),
            new Status(promotionProcess, Collections.emptyList()));

        Promotion promotion = promotionFuture.get();
        assertNotNull(promotionProcess.getBuilds().getLastBuild());
        assertEquals(promotion, promotionProcess.getBuilds().getLastBuild());
        r.assertBuildStatusSuccess(promotion);
        r.assertLogContains("prefix1Transferred", promotionProcess.getBuilds().getLastBuild());


        final Promotion lastBuild = promotionProcess.getBuilds().getLastBuild();
        final AbstractBuild<?, ?> rootBuild = lastBuild.getRootBuild();
        assertEquals("hudson.model.FreeStyleBuild", rootBuild.getClass().getCanonicalName());
        assertEquals("hudson.plugins.promoted_builds.Promotion", lastBuild.getClass().getCanonicalName());

        // bug JENKINS-59600 would only show in a web page, not through JenkinsRule
        try (WebClient webClient = r.createWebClient()) {
            final Page page = webClient.goTo(lastBuild.getUrl() + "/consoleText", "text/plain");
            r.assertStringContains(page.getWebResponse().getContentAsString(), "prefix1Transferred");
        }
    }

    private static class DummyBPClient implements BPClient<BPTransfer> {

        @Override
        public void setAbsoluteRemoteRoot(String absoluteRemoteRoot) {

        }

        @Override
        public boolean changeToInitialDirectory() {
            return false;
        }

        @Override
        public boolean changeDirectory(String directory) {
            return false;
        }

        @Override
        public boolean makeDirectory(String directory) {
            return false;
        }

        @Override
        public void beginTransfers(BPTransfer transfer) {

        }

        @Override
        public void deleteTree() throws Exception {

        }

        @Override
        public void transferFile(BPTransfer transfer, FilePath filePath, InputStream fileContent) throws Exception {

        }

        @Override
        public void endTransfers(BPTransfer transfer) {

        }

        @Override
        public void disconnect() throws Exception {

        }

        @Override
        public void disconnectQuietly() {

        }
    }

    private static class DummyBPHostConfiguration extends BPHostConfiguration<DummyBPClient, Object> {

        @Override
        public DummyBPClient createClient(BPBuildInfo buildInfo) {
            return new DummyBPClient();
        }
    }

    public static class DummyBPPlugin extends BPPlugin<BapPublisher<BPTransfer>, DummyBPClient, Object> {

        public DummyBPPlugin(String consolePrefix) {
            super(consolePrefix);
        }

        @Override
        public BPHostConfiguration<BPPluginTest.DummyBPClient, Object> getConfiguration(String name) {
            return new BPPluginTest.DummyBPHostConfiguration();
        }

    }
}
