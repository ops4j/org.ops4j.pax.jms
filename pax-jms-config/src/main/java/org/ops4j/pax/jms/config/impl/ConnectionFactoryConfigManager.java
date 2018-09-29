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

import org.jasypt.encryption.StringEncryptor;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.service.PooledConnectionFactoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches for ConnectionFactory configs in OSGi configuration admin and creates / destroys the trackers
 * for the ConnectionFactoryFactories and pooling support
 */
@SuppressWarnings({ "rawtypes" })
public class ConnectionFactoryConfigManager implements ManagedServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactoryConfigManager.class);

    private BundleContext context;
    private ExternalConfigLoader externalConfigLoader;

    /**
     * Stores one ServiceTracker for ConnectionFactoryFactories for each config pid
     */
    private Map<String, ServiceTracker<?, ?>> trackers;

    public ConnectionFactoryConfigManager(BundleContext context, ExternalConfigLoader externalConfigLoader) {
        this.context = context;
        this.externalConfigLoader = externalConfigLoader;
        this.trackers = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "connectionfactory";
    }

    @Override
    public void updated(final String pid, final Dictionary config) throws ConfigurationException {
        deleted(pid);
        if (config == null) {
            return;
        }

        Dictionary<String, Object> loadedConfig = externalConfigLoader.resolve(config);
        String seFilter = getStringEncryptorFilter(loadedConfig);
        String cffFilter = getCFFFilter(loadedConfig);
        String pcffFilter = getPooledCFFFilter(loadedConfig);

        ServiceTrackerHelper helper = ServiceTrackerHelper.helper(context);
        ServiceTracker<?, ?> tracker;

        if (Objects.nonNull(pcffFilter)) {
            tracker = helper.track(StringEncryptor.class, seFilter, se ->
                    helper.track(PooledConnectionFactoryFactory.class, pcffFilter, pcff ->
                            helper.track(ConnectionFactoryFactory.class, cffFilter, cff ->
                                            new ConnectionFactoryRegistration(context,
                                                    new PoolingWrapper(pcff, cff),
                                                    loadedConfig,
                                                    new Decryptor(se).decrypt(loadedConfig)),
                                    ConnectionFactoryRegistration::close)));
        } else {
            tracker = helper.track(StringEncryptor.class, seFilter, se ->
                    helper.track(ConnectionFactoryFactory.class, cffFilter, cff ->
                                    new ConnectionFactoryRegistration(context,
                                            cff,
                                            loadedConfig,
                                            new Decryptor(se).decrypt(loadedConfig)),
                            ConnectionFactoryRegistration::close));
        }

        trackers.put(pid, tracker);
    }

    static String getStringEncryptorFilter(Dictionary<String, Object> config) {
        if (Decryptor.isEncrypted(config)) {
            String alias = Decryptor.getAlias(config);
            return andFilter(eqFilter("objectClass", StringEncryptor.class.getName()),
                    eqFilter("alias", alias));
        }
        return null;
    }

    static String getCFFFilter(Dictionary<String, Object> config) throws ConfigurationException {
        String cffName = (String) config.get(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE);
        if (cffName == null) {
            throw new ConfigurationException(null,
                    "Could not determine provider to use. Specify the "
                            + ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE + " property.");
        }
        return andFilter(eqFilter("objectClass", ConnectionFactoryFactory.class.getName()),
                eqFilter(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, cffName));
    }

    static String getPooledCFFFilter(Dictionary<String, Object> config) throws ConfigurationException {
        String pool = (String) config.remove(PooledConnectionFactoryFactory.POOL_KEY);
        boolean isXa = isXa(config);
        if (pool == null) {
            if (isXa) {
                throw new ConfigurationException(null, "Can not create XA Connection Factory without pooling.");
            } else {
                return null;
            }
        }
        return andFilter(eqFilter("objectClass", PooledConnectionFactoryFactory.class.getName()),
                eqFilter("pool", pool),
                eqFilter("xa", Boolean.toString(isXa)));
    }

    static boolean isXa(Dictionary<String, Object> config) throws ConfigurationException {
        String xa = (String) config.remove(PooledConnectionFactoryFactory.XA_KEY);
        if (xa == null) {
            return false;
        } else {
            if ("true".equals(xa)) {
                return true;
            } else if ("false".equals(xa)) {
                return false;
            } else {
                throw new ConfigurationException(null, "Invalid XA configuration provided, XA can only be set to true or false");
            }
        }
    }

    static String eqFilter(String key, String value) {
        return value != null ? "(" + key + "=" + value + ")" : null;
    }

    static String andFilter(String... filterList) {
        String last = null;
        StringBuilder filter = new StringBuilder("(&");
        int count = 0;
        for (String filterPart : filterList) {
            if (filterPart != null) {
                last = filterPart;
                filter.append(filterPart);
                count++;
            }
        }
        filter.append(")");

        return count > 1 ? filter.toString() : last;
    }

    @Override
    public void deleted(String pid) {
        ServiceTracker<?, ?> tracker = trackers.remove(pid);
        if (tracker != null) {
            tracker.close();
        }
    }

    void destroy() {
        for (String pid : trackers.keySet()) {
            deleted(pid);
        }
    }

}
