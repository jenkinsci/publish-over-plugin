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

package jenkins.plugins.publish_over.helper;

import static org.mockito.Mockito.mock;
import jenkins.plugins.publish_over.BPBuildInfo;
import jenkins.plugins.publish_over.BPClient;
import jenkins.plugins.publish_over.BPHostConfiguration;

import java.io.Serial;

public class BPHostConfigurationFactory {

    public BPHostConfiguration create(final String configName) {
        final BPHostConfiguration config = new ConcreteBPHostConfiguration();
        config.setName(configName);
        return config;
    }

    public BPHostConfiguration create(final String configName, final BPClient client) {
        final BPHostConfiguration config = new ConcreteBPHostConfiguration(client);
        config.setName(configName);
        return config;
    }

    public static class ConcreteBPHostConfiguration<CLIENT extends BPClient> extends BPHostConfiguration<CLIENT, Object> {

        @Serial
        private static final long serialVersionUID = 1L;

        private final CLIENT client;

        public ConcreteBPHostConfiguration() {
            // beautiful :-(
            this((CLIENT) mock(BPClient.class));
        }

        public ConcreteBPHostConfiguration(final CLIENT client) {
            this.client = client;
        }

        @Override
        public CLIENT createClient(final BPBuildInfo buildInfo) {
            return client;
        }

        @Serial
        @Override
        public Object readResolve() {
            return super.readResolve();
        }
    }

}
