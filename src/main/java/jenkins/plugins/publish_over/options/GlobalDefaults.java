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

package jenkins.plugins.publish_over.options;

public class GlobalDefaults implements GlobalOptions  {

    public boolean isContinueOnError() {
        return false;
    }

    public boolean isFailOnError() {
        return false;
    }

    public boolean isAlwaysPublishFromMaster() {
        return false;
    }

    public String getParameterName() {
        return null;
    }

    public String getLabel() {
        return null;
    }

    public String getConfigName() {
        return null;
    }

    public boolean isUseWorkspaceInPromotion() {
        return false;
    }

    public boolean isUsePromotionTimestamp() {
        return false;
    }

    public boolean isVerbose() {
        return false;
    }

    public int getRetries() {
        return DEFAULT_RETRIES;
    }

    public long getRetryDelay() {
        return DEFAULT_RETRY_DELAY;
    }

    public String getSourceFiles() {
        return null;
    }

    public String getRemovePrefix() {
        return null;
    }

    public String getRemoteDirectory() {
        return null;
    }

    public String getExcludes() {
        return null;
    }

    public boolean isRemoteDirectorySDF() {
        return false;
    }

    public boolean isFlatten() {
        return false;
    }

    public boolean isCleanRemote() {
        return false;
    }

    public boolean isNoDefaultExcludes() {
        return false;
    }

    public boolean isMakeEmptyDirs() {
        return false;
    }

}
