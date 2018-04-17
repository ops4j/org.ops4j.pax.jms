/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.jms.config.impl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnectionFactory;

import org.jasypt.encryption.StringEncryptor;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.service.PooledConnectionFactoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class registers pooling {@link ConnectionFactory} created from
 * {@link org.ops4j.pax.jms.service.PooledConnectionFactoryFactory} using provided, existing
 * {@link ConnectionFactory}/{@link XAConnectionFactory}.
 */
public class ConnectionFactoryWrapper {

    public static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryWrapper.class);

    private ServiceTracker<?, ?> tracker;
    private Object cf;

    /**
     * A wrapper for connection factory service registered by application. This wrapper creates pooled {@link ConnectionFactory}
     * using properties that are part of original connection factory registration.
     *
     * @param context {@link BundleContext} of pax-jms-config
     * @param connectionFactory application registered and broker-specific connection factory
     * @param reference broker-specific {@link ServiceReference} for {@link ConnectionFactory}/{@link XAConnectionFactory}.
     */
    public ConnectionFactoryWrapper(BundleContext context, Object connectionFactory, ServiceReference<Object> reference) {
        LOG.info("Got service reference {}", connectionFactory);
        this.cf = connectionFactory;

        boolean xa = connectionFactory instanceof XAConnectionFactory;
        ConnectionFactoryFactory providedCFFactory = xa ? new ProvidedConnectionFactoryFactory((XAConnectionFactory) connectionFactory)
                : new ProvidedConnectionFactoryFactory((ConnectionFactory) connectionFactory);

        Dictionary<String, Object> config = serviceReferenceProperties(reference);
        Dictionary<String, Object> loadedConfig = new ExternalConfigLoader().resolve(config);
        loadedConfig.put("xa", Boolean.toString(xa));
        loadedConfig.put(Constants.SERVICE_RANKING, getInt(config, Constants.SERVICE_RANKING, 0) + 1000);
        // reference to service being wrapped
        loadedConfig.put("pax.jms.service.id.ref", config.get(Constants.SERVICE_ID));

        String seFilter = ConnectionFactoryConfigManager.getStringEncryptorFilter(loadedConfig);
        String pcffFilter = null;
        try {
            pcffFilter = ConnectionFactoryConfigManager.getPooledCFFFilter(loadedConfig);
        } catch (ConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        ServiceTrackerHelper helper = ServiceTrackerHelper.helper(context);

        if (pcffFilter == null) {
            throw new IllegalArgumentException("No pooling configuration available for service " + connectionFactory.toString()
                    + ": " + loadedConfig);
        }
        final String finalPcffFilter = pcffFilter;

        tracker = helper.track(StringEncryptor.class, seFilter, se ->
                helper.track(PooledConnectionFactoryFactory.class, finalPcffFilter, pcff ->
                                    new ConnectionFactoryRegistration(context,
                                            new PoolingWrapper(pcff, providedCFFactory),
                                            loadedConfig,
                                            new Decryptor(se).decrypt(loadedConfig)),
                                ConnectionFactoryRegistration::close));
    }

    /**
     * Gets {@link Dictionary} of properties from non-null {@link ServiceReference}
     * @param reference
     * @return
     */
    private Dictionary<String,Object> serviceReferenceProperties(ServiceReference<Object> reference) {
        Hashtable<String, Object> result = new Hashtable<>();
        if (reference != null) {
            for (String key : reference.getPropertyKeys()) {
                result.put(key, reference.getProperty(key));
            }
        }
        return result;
    }

    /**
     * Wrapper is closed when the original service is unregistered or if pax-jms-config bundle is stopped
     */
    public void close() {
        if (tracker != null) {
            if (cf != null) {
                LOG.info("Closed service reference: {}", this.cf);
            }
            tracker.close();
        }
    }

    private int getInt(Dictionary<String, Object> properties, String name, int defaultValue) {
        Object v = properties.get(name);
        if (v instanceof Integer) {
            return (Integer) v;
        } else if (v instanceof String) {
            return Integer.parseInt((String) v);
        } else {
            return defaultValue;
        }
    }

    /**
     * {@link ConnectionFactoryFactory} which doesn't create anything - just returns what it's configured with
     */
    private static final class ProvidedConnectionFactoryFactory implements ConnectionFactoryFactory {
        private ConnectionFactory providedCF;
        private XAConnectionFactory providedXACF;

        ProvidedConnectionFactoryFactory(ConnectionFactory providedConnectionFactory) {
            this.providedCF = providedConnectionFactory;
        }

        ProvidedConnectionFactoryFactory(XAConnectionFactory providedXAConnectionFactory) {
            this.providedXACF = providedXAConnectionFactory;
        }

        @Override
        public ConnectionFactory createConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
            return providedCF;
        }

        @Override
        public XAConnectionFactory createXAConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
            return providedXACF;
        }
    }

}
