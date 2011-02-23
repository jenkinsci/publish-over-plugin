/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over;

import jenkins.plugins.publish_over.helper.BPBuildInfoFactory;
import jenkins.plugins.publish_over.helper.BPHostConfigurationFactory;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods" })
public class BapPublisherTest {

    private final BPBuildInfo buildInfo = new BPBuildInfoFactory().createEmpty();
    private final IMocksControl mockControl = EasyMock.createStrictControl();
    private final BPClient mockClient = mockControl.createMock(BPClient.class);
    private final BPHostConfiguration hostConfiguration = new BPHostConfigurationFactory().create("TEST-CONFIG", mockClient);
    private final List<BPTransfer> transfers = new LinkedList<BPTransfer>();

    @Test public void testTransfersExecutedAndClientNotified() throws Exception {
        final int numberOfFilesTransferred1 = 2;
        final int numberOfFilesTransferred2 = 3;
        final int numberOfFilesTransferred3 = 4;
        final BPTransfer transfer1 = createHappyTransfer(numberOfFilesTransferred1);
        final BPTransfer transfer2 = createHappyTransfer(numberOfFilesTransferred2);
        final BPTransfer transfer3 = createHappyTransfer(numberOfFilesTransferred3);
        transfers.addAll(Arrays.asList(new BPTransfer[]{transfer1, transfer2, transfer3}));
        final BapPublisher publisher = new BapPublisher(hostConfiguration.getName(), false, transfers);
        mockClient.disconnectQuietly();

        mockControl.replay();
        publisher.perform(hostConfiguration, buildInfo);
        mockControl.verify();
    }

    private BPTransfer createHappyTransfer(final int numberOfFilesTransferred) throws Exception {
        final BPTransfer transfer = mockControl.createMock(BPTransfer.class);
        mockClient.beginTransfers(transfer);
        expect(transfer.hasConfiguredSourceFiles()).andReturn(true);
        expect(transfer.transfer(buildInfo, mockClient)).andReturn(numberOfFilesTransferred);
        mockClient.endTransfers(transfer);
        return transfer;
    }

    @Test public void testExceptionPropagatedAndClientDisconnected() throws Exception {
        final BPTransfer transfer1 = mockControl.createMock(BPTransfer.class);
        final BPTransfer transfer2 = mockControl.createMock(BPTransfer.class);
        transfers.addAll(Arrays.asList(new BPTransfer[]{transfer1, transfer2}));
        final BapPublisher publisher = new BapPublisher(hostConfiguration.getName(), false, transfers);
        final RuntimeException toThrow = new RuntimeException("xxx");
        mockClient.beginTransfers(transfer1);
        expect(transfer1.hasConfiguredSourceFiles()).andReturn(true);
        expect(transfer1.transfer(buildInfo, mockClient)).andThrow(toThrow);

        mockClient.disconnectQuietly();

        mockControl.replay();
        try {
            publisher.perform(hostConfiguration, buildInfo);
            fail();
        } catch (Exception t) {
            assertSame(toThrow, t);
        }
        mockControl.verify();
    }

    @Test public void testVerbositySetInBuildInfo() throws Exception {
        final BapPublisher publisher = new BapPublisher(null, false, null);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertFalse(buildInfo.isVerbose());
        publisher.setVerbose(true);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertTrue(buildInfo.isVerbose());
    }

    @Test public void testEnvironmentUntouchedIfNotPromotion() {
        assertNotSame(buildInfo, buildInfo.getCurrentBuildEnv());
        final BapPublisher publisher = new BapPublisher(null, false, null);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getCurrentBuildEnv().getEnvVars(), buildInfo.getEnvVars());
        assertSame(buildInfo.getCurrentBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getCurrentBuildEnv().getBuildTime(), buildInfo.getBuildTime());
    }

    @Test public void testEnvironmentIsTargetBuildIfInPromotion() {
        final BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo.setTargetBuildEnv(target);
        final String promoJobName = "promo";
        final String targetJobName = "job";
        final String envVarName = "JOB_NAME";
        target.getEnvVars().put(envVarName, targetJobName);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(envVarName, promoJobName);
        assertNotSame(buildInfo, target);
        final BapPublisher publisher = new BapPublisher(null, false, null);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getTargetBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getTargetBuildEnv().getBuildTime(), buildInfo.getBuildTime());
        assertEquals(buildInfo.getEnvVars().get(envVarName), targetJobName);
        assertEquals(buildInfo.getEnvVars().get(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX + envVarName), promoJobName);
    }

    @Test public void testEnvironmentCanSelectWorkspaceForPromotion() {
        final BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo.setTargetBuildEnv(target);
        final String promoJobName = "promo";
        final String targetJobName = "job";
        final String envVarName = "JOB_NAME";
        target.getEnvVars().put(envVarName, targetJobName);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(envVarName, promoJobName);
        assertNotSame(buildInfo, target);
        final BapPublisher publisher = new BapPublisher(null, false, null, true, false);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getCurrentBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getTargetBuildEnv().getBuildTime(), buildInfo.getBuildTime());
        assertEquals(buildInfo.getEnvVars().get(envVarName), targetJobName);
        assertEquals(buildInfo.getEnvVars().get(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX + envVarName), promoJobName);
    }

    @Test public void testEnvironmentCanSelectPromotionTimestampForRemoteDir() {
        final BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo.setTargetBuildEnv(target);
        final String promoJobName = "promo";
        final String targetJobName = "job";
        final String envVarName = "JOB_NAME";
        target.getEnvVars().put(envVarName, targetJobName);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(envVarName, promoJobName);
        assertNotSame(buildInfo, target);
        final BapPublisher publisher = new BapPublisher(null, false, null, false, true);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getTargetBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getCurrentBuildEnv().getBuildTime(), buildInfo.getBuildTime());
        assertEquals(buildInfo.getEnvVars().get(envVarName), targetJobName);
        assertEquals(buildInfo.getEnvVars().get(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX + envVarName), promoJobName);
    }

}
