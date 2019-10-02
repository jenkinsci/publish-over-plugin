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
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BPValidators {

    private static final String VALID_NAME_ILLEGAL_CHARS = "< & ' \" \\";
    private static final Pattern FOUR_NUMBERS_DOT_DELIM = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
    private static final int OCTETS_IN_IPV4 = 4;
    private static final int OCTET_MAX_VALUE = 255;

    public static String getIllegalCharacters() {
        return VALID_NAME_ILLEGAL_CHARS;
    }

    public static boolean isValidName(final String proposed) {
        return Util.fixEmptyAndTrim(proposed) != null && proposed.matches("[^<&'\"\\\\]+");
    }

    public static FormValidation validateName(final String name) {
        return isValidName(name) ? FormValidation.ok() : FormValidation.error(Messages.validator_safeName(VALID_NAME_ILLEGAL_CHARS));
    }

    public static FormValidation validateOptionalIp(final String ipAddress) {
        if (Util.fixEmptyAndTrim(ipAddress) == null) return FormValidation.ok();
        final Matcher matcher = FOUR_NUMBERS_DOT_DELIM.matcher(ipAddress.trim());
        if (!matcher.matches()) return FormValidation.error(Messages.validator_optionalIP());
        for (int octet = 1; octet < OCTETS_IN_IPV4 + 1; octet++)
            if (!isOctetValid(matcher.group(octet)))
                return FormValidation.error(Messages.validator_optionalIP());
        return FormValidation.ok();
    }

    private static boolean isOctetValid(final String octetString) {
        final int octet = Integer.parseInt(octetString);
        return octet < OCTET_MAX_VALUE + 1;
    }

    public static FormValidation validateFileOnMaster(final String value) {
        // this check prevents a NPE when called from global configuration - if not global, use validatePath directly
        if (JenkinsCapabilities.missing(JenkinsCapabilities.VALIDATE_FILE_ON_MASTER_FROM_GLOBAL_CFG))
            return FormValidation.ok();
        try {
            Jenkins jenkins = Jenkins.getInstance();
            if(jenkins == null) {
                return FormValidation.error("Unable to access Jenkins instance");
            }
            return jenkins.getRootPath().validateRelativePath(value, true, true);
        } catch (IOException ioe) {
            return FormValidation.error(ioe, "");
        }
    }

    public static FormValidation validateRegularExpression(final String value) {
        try {
            Pattern.compile(value);
            return FormValidation.ok();
        } catch (PatternSyntaxException pse) {
            return FormValidation.error(pse, Messages.validator_regularExpression(pse.getLocalizedMessage()));
        }
    }

}
