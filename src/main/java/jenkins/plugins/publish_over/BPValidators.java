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

import hudson.Util;
import hudson.util.FormValidation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BPValidators {

    private static final String VALID_NAME_ILLEGAL_CHARS = "< & ' \" \\";
    private static final Pattern FOUR_NUMBERS_DOT_DELIM = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    public static String getIllegalCharacters() {
        return VALID_NAME_ILLEGAL_CHARS;
    }

    public static boolean isValidName(final String proposed) {
        return Util.fixEmptyAndTrim(proposed) != null && proposed.matches("[^<&'\"\\\\]+");
    }

    public static FormValidation validateName(final String name) {
        return isValidName(name) ? FormValidation.ok() : FormValidation.error(Messages.validator_safeName(VALID_NAME_ILLEGAL_CHARS));
    }

    public static FormValidation validateOptionalIp(final String ip) {
        if (Util.fixEmptyAndTrim(ip) == null) return FormValidation.ok();
        final Matcher matcher = FOUR_NUMBERS_DOT_DELIM.matcher(ip.trim());
        if (!matcher.matches()) return FormValidation.error(Messages.validator_optionalIP());
        if (isOctetValid(matcher.group(1)) && isOctetValid(matcher.group(2)) && isOctetValid(matcher.group(3))
                && isOctetValid(matcher.group(4))) return FormValidation.ok();
        return FormValidation.error(Messages.validator_optionalIP());
    }

    private static boolean isOctetValid(final String octetString) {
        final int octet = Integer.parseInt(octetString);
        return octet < 256;
    }

}
