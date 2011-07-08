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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ParamPublish implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String parameterName;

    public ParamPublish(final String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public PubSelector createSelector(final BPBuildInfo buildInfo) {
        if (Util.fixEmptyAndTrim(parameterName) == null)
            return new SelectAllPubSelector();
        final String regexp = buildInfo.getCurrentBuildEnv().getEnvVars().get(parameterName);
        if (regexp == null)
            throw new BapPublisherException(Messages.exception_paramPublish_noParameter(parameterName));
        try {
            final Pattern pattern = Pattern.compile(regexp);
            return new Selector(buildInfo, pattern);
        } catch (PatternSyntaxException pse) {
            throw new BapPublisherException(Messages.exception_paramPublish_badPattern(parameterName, regexp, pse.getMessage()), pse);
        }
    }

    protected HashCodeBuilder addToHashCode(final HashCodeBuilder builder) {
        return builder.append(parameterName);
    }

    protected EqualsBuilder addToEquals(final EqualsBuilder builder, final ParamPublish that) {
        return builder.append(parameterName, that.parameterName);
    }

    protected ToStringBuilder addToToString(final ToStringBuilder builder) {
        return builder.append("parameterName", parameterName);
    }

    public boolean equals(final Object that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;

        return addToEquals(new EqualsBuilder(), (ParamPublish) that).isEquals();
    }

    public int hashCode() {
        return addToHashCode(new HashCodeBuilder()).toHashCode();
    }

    public String toString() {
        return addToToString(new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)).toString();
    }

    public static final class Selector implements PubSelector {

        private static final long serialVersionUID = 1L;

        private final BPBuildInfo buildInfo;
        private final Pattern pattern;

        public Selector(final BPBuildInfo buildInfo, final Pattern pattern) {
            this.buildInfo = buildInfo;
            this.pattern = pattern;
        }

        public boolean selected(final BapPublisher publisher) {
            String label = null;
            if ((publisher.getLabel() == null) || (Util.fixEmptyAndTrim(publisher.getLabel().getLabel()) == null)) {
                label = "";
            } else {
                final String rawLabel = publisher.getLabel().getLabel().trim();
                label = Util.replaceMacro(rawLabel, buildInfo.getEnvVars());
            }
            if (pattern.matcher(label).matches()) {
                buildInfo.println(Messages.console_paramPublish_match(label, pattern.pattern(), publisher.getConfigName()));
                return true;
            } else {
                buildInfo.println(Messages.console_paramPublish_skip(label, pattern.pattern(), publisher.getConfigName()));
                return false;
            }
        }

    }

}
