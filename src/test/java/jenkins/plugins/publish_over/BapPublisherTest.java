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

import hudson.FilePath;
import jenkins.plugins.publish_over.helper.BPBuildInfoFactory;
import jenkins.plugins.publish_over.helper.BPHostConfigurationFactory;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods", "PMD.LooseCoupling"}) 
class BapPublisherTest {

    private static final Logger PUBLISHER_LOGGER = Logger.getLogger(BapPublisher.class.getCanonicalName());
    private static Level originalLogLevel;

    @BeforeAll 
    static void beforeAll() {
        originalLogLevel = PUBLISHER_LOGGER.getLevel();
        PUBLISHER_LOGGER.setLevel(Level.OFF);
    }

    @AfterAll
    static void afterAll() {
        PUBLISHER_LOGGER.setLevel(originalLogLevel);
    }

    private final BPBuildInfo buildInfo = new BPBuildInfoFactory().createEmpty();
    private final IMocksControl mockControl = EasyMock.createStrictControl();
    private final BPClient mockClient = mockControl.createMock(BPClient.class);
    private final BPHostConfiguration hostConfiguration = new BPHostConfigurationFactory().create("TEST-CONFIG", mockClient);
    private final ArrayList<BPTransfer> transfers = new ArrayList<>();

    @Test
    void testTransfersExecutedAndClientNotified() throws Exception {
        final int numberOfFilesTransferred1 = 2;
        final int numberOfFilesTransferred2 = 3;
        final int numberOfFilesTransferred3 = 4;
        final BPTransfer transfer1 = createHappyTransfer(numberOfFilesTransferred1);
        final BPTransfer transfer2 = createHappyTransfer(numberOfFilesTransferred2);
        final BPTransfer transfer3 = createHappyTransfer(numberOfFilesTransferred3);
        transfers.addAll(Arrays.asList(transfer1, transfer2, transfer3));
        final BapPublisher publisher = createPublisher(hostConfiguration.getName(), false, transfers);
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

    @Test
    void testExceptionPropagatedAndClientDisconnected() throws Exception {
        final BPTransfer transfer1 = mockControl.createMock(BPTransfer.class);
        final BPTransfer transfer2 = mockControl.createMock(BPTransfer.class);
        transfers.addAll(Arrays.asList(transfer1, transfer2));
        final BapPublisher publisher = createPublisher(hostConfiguration.getName(), false, transfers);
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

    @Test
    void testVerbositySetInBuildInfo() {
        final BapPublisher publisher = createPublisher(null, false, null);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertFalse(buildInfo.isVerbose());
        publisher.setVerbose(true);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertTrue(buildInfo.isVerbose());
    }

    @Test
    void testEnvironmentUntouchedIfNotPromotion() {
        assertNotSame(buildInfo, buildInfo.getCurrentBuildEnv());
        final BapPublisher publisher = createPublisher(null, false, null);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getCurrentBuildEnv().getEnvVars(), buildInfo.getEnvVars());
        assertSame(buildInfo.getCurrentBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getCurrentBuildEnv().getBuildTime(), buildInfo.getBuildTime());
    }

    @Test
    void testEnvironmentIsTargetBuildIfInPromotion() {
        final BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo.setTargetBuildEnv(target);
        final String promoJobName = "promo";
        final String targetJobName = "job";
        final String envVarName = "JOB_NAME";
        target.getEnvVars().put(envVarName, targetJobName);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(envVarName, promoJobName);
        assertNotSame(buildInfo, target);
        final BapPublisher publisher = createPublisher(null, false, null);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getTargetBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getTargetBuildEnv().getBuildTime(), buildInfo.getBuildTime());
        assertEquals(targetJobName, buildInfo.getEnvVars().get(envVarName));
        assertEquals(promoJobName, buildInfo.getEnvVars().get(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX + envVarName));
    }

    @Test
    void testEnvironmentCanSelectWorkspaceForPromotion() {
        final BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo.setTargetBuildEnv(target);
        final String promoJobName = "promo";
        final String targetJobName = "job";
        final String envVarName = "JOB_NAME";
        target.getEnvVars().put(envVarName, targetJobName);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(envVarName, promoJobName);
        assertNotSame(buildInfo, target);
        final BapPublisher publisher = createPublisher(null, false, null, true, false);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getCurrentBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getTargetBuildEnv().getBuildTime(), buildInfo.getBuildTime());
        assertEquals(targetJobName, buildInfo.getEnvVars().get(envVarName));
        assertEquals(promoJobName, buildInfo.getEnvVars().get(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX + envVarName));
    }

    @Test
    void testEnvironmentCanSelectPromotionTimestampForRemoteDir() {
        final BPBuildEnv target = new BPBuildInfoFactory().createEmptyBuildEnv();
        buildInfo.setTargetBuildEnv(target);
        final String promoJobName = "promo";
        final String targetJobName = "job";
        final String envVarName = "JOB_NAME";
        target.getEnvVars().put(envVarName, targetJobName);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(envVarName, promoJobName);
        assertNotSame(buildInfo, target);
        final BapPublisher publisher = createPublisher(null, false, null, false, true);
        publisher.setEffectiveEnvironmentInBuildInfo(buildInfo);
        assertSame(buildInfo.getTargetBuildEnv().getBaseDirectory(), buildInfo.getBaseDirectory());
        assertSame(buildInfo.getCurrentBuildEnv().getBuildTime(), buildInfo.getBuildTime());
        assertEquals(targetJobName, buildInfo.getEnvVars().get(envVarName));
        assertEquals(promoJobName, buildInfo.getEnvVars().get(BPBuildInfo.PROMOTION_ENV_VARS_PREFIX + envVarName));
    }

    @Test
    void testRetry() throws Exception {
        final int retries = 1;
        final long retryDelay = 100;
        final BPTransfer transfer = mockControl.createMock(BPTransfer.class);
        transfers.add(transfer);
        mockClient.beginTransfers(transfer);
        expect(transfer.hasConfiguredSourceFiles()).andReturn(true);
        final BPTransfer.TransferState state = BPTransfer.TransferState.create(new FileFinderResult(new FilePath[0], new FilePath[0]));
        final BapTransferException bte = new BapTransferException(new IOException(), state);
        expect(transfer.transfer(buildInfo, mockClient)).andThrow(bte);
        mockClient.disconnectQuietly();
        mockClient.beginTransfers(transfer);
        expect(transfer.hasConfiguredSourceFiles()).andReturn(true);
        expect(transfer.transfer(buildInfo, mockClient, state)).andReturn(1);
        mockClient.endTransfers(transfer);
        mockClient.disconnectQuietly();
        final Retry retry = new Retry(retries, retryDelay);
        final BapPublisher publisher = createPublisher(hostConfiguration.getName(), false, transfers, false, false, retry);

        mockControl.replay();
        publisher.perform(hostConfiguration, buildInfo);
        mockControl.verify();
    }

    @Test
    void testTransferExceptionCauseIsReThrownWhenRetriesExhausted() throws Exception {
        final int retries = 1;
        final long retryDelay = 100;
        final BPTransfer transfer = mockControl.createMock(BPTransfer.class);
        transfers.add(transfer);
        mockClient.beginTransfers(transfer);
        expect(transfer.hasConfiguredSourceFiles()).andReturn(true);
        final BPTransfer.TransferState state = BPTransfer.TransferState.create(new FileFinderResult(new FilePath[0], new FilePath[0]));
        final BapTransferException bte = new BapTransferException(new IOException(), state);
        expect(transfer.transfer(buildInfo, mockClient)).andThrow(bte);
        mockClient.disconnectQuietly();
        mockClient.beginTransfers(transfer);
        expect(transfer.hasConfiguredSourceFiles()).andReturn(true);
        final IOException expected = new IOException("It was all baaad");
        expect(transfer.transfer(buildInfo, mockClient, state)).andThrow(new BapTransferException(expected, state));
        mockClient.disconnectQuietly();
        final Retry retry = new Retry(retries, retryDelay);
        final BapPublisher publisher = createPublisher(hostConfiguration.getName(), false, transfers, false, false, retry);

        mockControl.replay();
        try {
            publisher.perform(hostConfiguration, buildInfo);
            fail();
        } catch (IOException ioe) {
            assertSame(expected, ioe);
        }
        mockControl.verify();
    }

    @Test
    void testExceptionIsReThrownWhenRetriesExhausted() throws Exception {
        final int retries = 1;
        final long retryDelay = 100;
        final BPTransfer transfer = mockControl.createMock(BPTransfer.class);
        transfers.add(transfer);
        mockClient.beginTransfers(transfer);
        expectLastCall().andThrow(new RuntimeException("Ouch!"));
        mockClient.disconnectQuietly();
        mockClient.beginTransfers(transfer);
        final RuntimeException expected = new RuntimeException("Unexpected :-/");
        expectLastCall().andThrow(expected);
        mockClient.disconnectQuietly();
        final Retry retry = new Retry(retries, retryDelay);
        final BapPublisher publisher = createPublisher(hostConfiguration.getName(), false, transfers, false, false, retry);

        mockControl.replay();
        try {
            publisher.perform(hostConfiguration, buildInfo);
            fail();
        } catch (RuntimeException re) {
            assertSame(expected, re);
        }
        mockControl.verify();
    }

    private static BapPublisher createPublisher(final String configName, final boolean verbose, final ArrayList<BPTransfer> transfers) {
        return createPublisher(configName, verbose, transfers, false, false);
    }

    private static BapPublisher createPublisher(final String configName, final boolean verbose, final ArrayList<BPTransfer> transfers,
                                                final boolean useWorkspace, final boolean usePromotionTimestamp) {
        return createPublisher(configName, verbose, transfers, useWorkspace, usePromotionTimestamp, null);
    }

    private static BapPublisher createPublisher(final String configName, final boolean verbose, final ArrayList<BPTransfer> transfers,
                                                final boolean useWorkspace, final boolean usePromotionTimestamp, final Retry retry) {
        return new BapPublisher(configName, verbose, transfers, useWorkspace, usePromotionTimestamp, retry, null, null);
    }

}
