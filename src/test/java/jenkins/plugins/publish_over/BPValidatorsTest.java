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

import org.junit.jupiter.api.Test;

import static hudson.util.FormValidation.Kind.ERROR;
import static hudson.util.FormValidation.Kind.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidUsingHardCodedIP"})
class BPValidatorsTest {

    @Test
    void validateNameSingleCharacterOk() {
        assertValidateNameOk("a");
    }

    @Test
    void validateNameWithWhitespaceOk() {
        assertValidateNameOk(" hello world ");
    }

    @Test
    void validateNameNullNotOk() {
        assertValidateNameNok(null);
    }

    @Test
    void validateNameAllWhitespaceNotOk() {
        assertValidateNameNok("  ");
    }

    @Test
    void validateNameLessThanNotOk() {
        assertValidateNameNok("<");
    }

    @Test
    void validateNameWithLessThanNotOk() {
        assertValidateNameNok("myname<isnotok");
    }

    @Test
    void validateNameWithAmpNotOk() {
        assertValidateNameNok("myname&isnotok");
    }

    @Test
    void validateNameWithSingleQuoteNotOk() {
        assertValidateNameNok("myname'isnotok");
    }

    @Test
    void validateNameWithDoubleQuoteNotOk() {
        assertValidateNameNok("myname\"isnotok");
    }

    @Test
    void validateNameWithBackSlashNotOk() {
        assertValidateNameNok("myname\\isnotok");
    }

    private void assertValidateNameOk(final String name) {
        assertEquals(OK, BPValidators.validateName(name).kind);
    }

    private void assertValidateNameNok(final String name) {
        assertEquals(ERROR, BPValidators.validateName(name).kind);
    }

    @Test
    void validateOptionalIpNullOk() {
        assertValidateOptionalIpOk(null);
    }

    @Test
    void validateOptionalIpEmptyStringOk() {
        assertValidateOptionalIpOk("");
    }

    @Test
    void validateOptionalIpAllWhitespaceOk() {
        assertValidateOptionalIpOk("  ");
    }

    @Test
    void validateOptionalIpValidIpPatternOk() {
        assertValidateOptionalIpOk("1.2.3.4");
    }

    @Test
    void validateOptionalIpValidIpPatternMaxOk() {
        assertValidateOptionalIpOk("255.255.255.255");
    }

    @Test
    void validateOptionalIpValidIpWhitespaceSurroundOk() {
        assertValidateOptionalIpOk(" 255.255.255.255   ");
    }

    @Test
    void validateOptionalIpOctetOver255Nok() {
        assertValidateOptionalIpNok("255.255.256.255");
    }

    @Test
    void validateOptionalIpOctetNegativeNok() {
        assertValidateOptionalIpNok("255.255.-1.255");
    }

    @Test
    void validateOptionalIpNotEnoughParts() {
        assertValidateOptionalIpNok("255.255.33");
    }

    private void assertValidateOptionalIpOk(final String ipAddress) {
        assertEquals(OK, BPValidators.validateOptionalIp(ipAddress).kind);
    }

    private void assertValidateOptionalIpNok(final String ipAddress) {
        assertEquals(ERROR, BPValidators.validateOptionalIp(ipAddress).kind);
    }

}
