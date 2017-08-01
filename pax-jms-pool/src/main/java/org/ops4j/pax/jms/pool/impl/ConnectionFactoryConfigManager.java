/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.ops4j.pax.jms.pool.impl;

import org.jasypt.encryption.StringEncryptor;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.transx.tm.TransactionManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * Watches for DataSource configs in OSGi configuration admin and creates / destroys the trackers
 * for the DataSourceFactories and pooling support
 */
@SuppressWarnings({ "rawtypes"})
public class ConnectionFactoryConfigManager implements ManagedServiceFactory {

    private Logger LOG = LoggerFactory.getLogger(ConnectionFactoryConfigManager.class);
    private BundleContext context;

    /**
     * Stores one ServiceTracker for DataSourceFactories for each config pid
     */
    private Map<String, ServiceTracker<?, ?>> trackers;

    public ConnectionFactoryConfigManager(BundleContext context) {
        this.context = context;
        this.trackers = new HashMap<>();
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

        Dictionary<String, Object> loadedConfig = new ExternalConfigLoader().resolve(config);
        String seFilter = getStringEncryptorFilter(loadedConfig);
        String cffFilter = getCFFFilter(loadedConfig);

        ServiceTrackerHelper helper = ServiceTrackerHelper.helper(context);
        ServiceTracker<?,?> tracker;

        tracker = helper.track(StringEncryptor.class, seFilter, se ->
                      helper.track(TransactionManager.class, tm ->
                          helper.track(ConnectionFactoryFactory.class, cffFilter, cff ->
                              new ConnectionFactoryRegistration(context,
                                                                cff,
                                                                tm,
                                                                loadedConfig,
                                                                new Decryptor(se).decrypt(loadedConfig)),
                              ConnectionFactoryRegistration::close)));

        trackers.put(pid, tracker);
    }


    private String getStringEncryptorFilter(Dictionary<String, Object> config) throws ConfigurationException {
        if (Decryptor.isEncrypted(config)) {
            String alias = Decryptor.getAlias(config);
            return andFilter(eqFilter("objectClass", StringEncryptor.class.getName()),
                             eqFilter("alias", alias));
        }
        return null;
    }

    private String getCFFFilter(Dictionary config) throws ConfigurationException {
        String cffName = (String) config.get(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME);
        if (cffName == null) {
            throw new ConfigurationException(null,
                "Could not determine provider to use. Specify the "
                    + ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME + " property.");
        }
        return andFilter(eqFilter("objectClass", ConnectionFactoryFactory.class.getName()),
                         eqFilter(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, cffName));
    }

    private String eqFilter(String key, String value) {
        return value != null ? "(" + key + "=" + value + ")" : null;
    }

    private String andFilter(String... filterList) {
        String last = null;
        StringBuilder filter = new StringBuilder("(&");
        int count = 0;
        for (String filterPart : filterList) {
            if (filterPart != null) {
                last = filterPart;
                filter.append(filterPart);
                count ++;
            }
        }
        filter.append(")");

        return count > 1 ? filter.toString() : last;
    }

    @Override
    public void deleted(String pid) {
        ServiceTracker<?,?> tracker = trackers.remove(pid);
        if (tracker != null) {
            tracker.close();
        }
    }

}
