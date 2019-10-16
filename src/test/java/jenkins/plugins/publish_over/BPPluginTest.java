package jenkins.plugins.publish_over;

import hudson.FilePath;
import hudson.Functions;
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
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BPPluginTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    public static class DummyBPPlugin extends BPPlugin {

        public DummyBPPlugin(String consolePrefix) {
            super(consolePrefix);
        }

        @Override
        public BPHostConfiguration getConfiguration(String name) {
            return new BPHostConfiguration() {
                @Override
                public BPClient createClient(BPBuildInfo buildInfo) {
                    return new BPClient() {
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
                    };
                }
            };
        }
    }


    @Test
    public void testNormalBuild() throws Exception {

        // create project
        FreeStyleProject freeStyleProject = r.createFreeStyleProject("proj1");
        freeStyleProject.getBuildersList().add(Functions.isWindows()
                ? new BatchFile("echo Hello, world > file.txt")
                : new Shell("echo Hello, world > file.txt"));
        freeStyleProject.getPublishersList().add(new ArtifactArchiver("file.txt"));

        BapPublisher bapPublisher = new BapPublisher();
        bapPublisher.setTransfers(new ArrayList());
        bapPublisher.getTransfers().add(new BPTransfer("file.txt", null, null, null, false, false));

        BPPlugin bpPlugin = new DummyBPPlugin("prefix1");
        bpPlugin.getDelegate().setPublishers(new ArrayList());
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
        BapPublisher bapPublisher = new BapPublisher();
        bapPublisher.setTransfers(new ArrayList());
        bapPublisher.getTransfers().add(new BPTransfer("file.txt", null, null, null, false, false));

        BPPlugin bpPlugin = new DummyBPPlugin("prefix1");
        bpPlugin.getDelegate().setPublishers(new ArrayList());
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

        // trigger promotion build
        assertEquals(0, promotionProcess.getBuilds().size());
        Future<Promotion> promotionFuture = promotionProcess.promote2(freeStyleBuild, new Cause.UserIdCause(), new Status(promotionProcess, Arrays.asList()));

        Promotion promotion = promotionFuture.get();
        assertEquals(1, promotionProcess.getBuilds().size());

        r.assertBuildStatusSuccess(promotion);
    }

}
