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
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.PatternSyntaxException;

import static org.easymock.EasyMock.expect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods"})
class ParamPublishTest {

    private static final String PARAM_NAME = "PUBLISHERS";
    private final BPBuildInfo buildInfo = new BPBuildInfoFactory().createEmpty();
    private final IMocksControl mockControl = EasyMock.createStrictControl();

    @BeforeEach
    void beforeEach() {
        mockControl.checkOrder(false);
        buildInfo.setEnvVars(buildInfo.getCurrentBuildEnv().getEnvVars());
    }

    @Test
    void testSelector() {
        final ParamPublish paramPublish = createParamPublish("[AC]");
        final BapPublisher pubA = createMockPublisher("A");
        final BapPublisher pubB = createMockPublisher("B");
        final BapPublisher pubC = createMockPublisher("C");
        mockControl.replay();
        final PubSelector selector = paramPublish.createSelector(buildInfo);
        assertTrue(selector.selected(pubA));
        assertFalse(selector.selected(pubB));
        assertTrue(selector.selected(pubC));
        mockControl.verify();
    }

    private ParamPublish createParamPublish(final String regex) {
        final ParamPublish paramPublish = new ParamPublish(PARAM_NAME);
        buildInfo.getCurrentBuildEnv().getEnvVars().put(PARAM_NAME, regex);
        return paramPublish;
    }

    private BapPublisher createMockPublisher(final String name) {
        final BapPublisher mockPublisher = mockControl.createMock(BapPublisher.class);
        expect(mockPublisher.getLabel()).andReturn(new PublisherLabel(name)).anyTimes();
        expect(mockPublisher.getConfigName()).andReturn("Meh").anyTimes();
        return mockPublisher;
    }

    @Test
    void testNoLabelIsEqualToEmptyString() {
        final BapPublisher emptyLabelName = createMockPublisher("");
        final BapPublisher nullLabelName = createMockPublisher(null);
        final BapPublisher nullLabel = mockControl.createMock(BapPublisher.class);
        expect(nullLabel.getLabel()).andReturn(null).anyTimes();
        expect(nullLabel.getConfigName()).andReturn("dave").anyTimes();
        mockControl.replay();
        final PubSelector noMatch = createParamPublish("NO_MATCH").createSelector(buildInfo);
        assertFalse(noMatch.selected(emptyLabelName));
        assertFalse(noMatch.selected(nullLabelName));
        assertFalse(noMatch.selected(nullLabel));
        final PubSelector matchEmpty = createParamPublish("").createSelector(buildInfo);
        assertTrue(matchEmpty.selected(emptyLabelName));
        assertTrue(matchEmpty.selected(nullLabelName));
        assertTrue(matchEmpty.selected(nullLabel));
        final PubSelector matchAny = createParamPublish(".*").createSelector(buildInfo);
        assertTrue(matchAny.selected(emptyLabelName));
        assertTrue(matchAny.selected(nullLabelName));
        assertTrue(matchAny.selected(nullLabel));
        mockControl.verify();
    }

    @Test
    void testBadPattern() {
        final String regex = "this should fail(";
        final ParamPublish paramPublish = createParamPublish(regex);
        try {
            paramPublish.createSelector(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertTrue(bpe.getLocalizedMessage().contains(PARAM_NAME));
            assertTrue(bpe.getLocalizedMessage().contains(regex));
            assertInstanceOf(PatternSyntaxException.class, bpe.getCause());
        }
    }

    @Test
    void testNoParameter() {
        final ParamPublish paramPublish = new ParamPublish(PARAM_NAME);
        try {
            paramPublish.createSelector(buildInfo);
            fail();
        } catch (BapPublisherException bpe) {
            assertEquals(Messages.exception_paramPublish_noParameter(PARAM_NAME), bpe.getLocalizedMessage());
        }
    }

    @Test
    void testLabelCanUseEnvVars() {
        final String nodeName = "master";
        final ParamPublish paramPublish = createParamPublish(nodeName);
        final BapPublisher pub = createMockPublisher("$" + BPBuildEnv.ENV_NODE_NAME);
        buildInfo.getEnvVars().put(BPBuildEnv.ENV_NODE_NAME, nodeName);
        mockControl.replay();
        final PubSelector selector = paramPublish.createSelector(buildInfo);
        assertTrue(selector.selected(pub));
        mockControl.verify();
    }

}
