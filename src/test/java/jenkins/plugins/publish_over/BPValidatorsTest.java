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

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static hudson.util.FormValidation.Kind.ERROR;
import static hudson.util.FormValidation.Kind.OK;

@SuppressWarnings({ "PMD.TooManyMethods", "PMD.AvoidUsingHardCodedIP" })
public class BPValidatorsTest {

    @Test public void validateNameSingleCharacterOk() {
        assertValidateNameOk("a");
    }

    @Test public void validateNameWithWhitespaceOk() {
        assertValidateNameOk(" hello world ");
    }

    @Test public void validateNameNullNotOk() {
        assertValidateNameNok(null);
    }

    @Test public void validateNameAllWhitespaceNotOk() {
        assertValidateNameNok("  ");
    }

    @Test public void validateNameLessThanNotOk() {
        assertValidateNameNok("<");
    }

    @Test public void validateNameWithLessThanNotOk() {
        assertValidateNameNok("myname<isnotok");
    }

    @Test public void validateNameWithAmpNotOk() {
        assertValidateNameNok("myname&isnotok");
    }

    @Test public void validateNameWithSingleQuoteNotOk() {
        assertValidateNameNok("myname'isnotok");
    }

    @Test public void validateNameWithDoubleQuoteNotOk() {
        assertValidateNameNok("myname\"isnotok");
    }

    @Test public void validateNameWithBackSlashNotOk() {
        assertValidateNameNok("myname\\isnotok");
    }

    private void assertValidateNameOk(final String name) {
        assertEquals(OK, BPValidators.validateName(name).kind);
    }

    private void assertValidateNameNok(final String name) {
        assertEquals(ERROR, BPValidators.validateName(name).kind);
    }

    @Test public void validateOptionalIpNullOk() {
        assertValidateOptionalIpOk(null);
    }

    @Test public void validateOptionalIpEmptyStringOk() {
        assertValidateOptionalIpOk("");
    }

    @Test public void validateOptionalIpAllWhitespaceOk() {
        assertValidateOptionalIpOk("  ");
    }

    @Test public void validateOptionalIpValidIpPatternOk() {
        assertValidateOptionalIpOk("1.2.3.4");
    }

    @Test public void validateOptionalIpValidIpPatternMaxOk() {
        assertValidateOptionalIpOk("255.255.255.255");
    }

    @Test public void validateOptionalIpValidIpWhitespaceSurroundOk() {
        assertValidateOptionalIpOk(" 255.255.255.255   ");
    }

    @Test public void validateOptionalIpOctetOver255Nok() {
        assertValidateOptionalIpNok("255.255.256.255");
    }

    @Test public void validateOptionalIpOctetNegativeNok() {
        assertValidateOptionalIpNok("255.255.-1.255");
    }

    @Test public void validateOptionalIpNotEnoughParts() {
        assertValidateOptionalIpNok("255.255.33");
    }

    private void assertValidateOptionalIpOk(final String ip) {
        assertEquals(OK, BPValidators.validateOptionalIp(ip).kind);
    }

    private void assertValidateOptionalIpNok(final String ip) {
        assertEquals(ERROR, BPValidators.validateOptionalIp(ip).kind);
    }

}
