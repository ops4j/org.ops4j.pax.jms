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

import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.transx.jms.ManagedConnectionFactoryBuilder;
import org.ops4j.pax.transx.tm.TransactionManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings({
    "rawtypes", "unchecked"
})
public class ConnectionFactoryRegistration implements Closeable {

    static final String JNDI_SERVICE_NAME = "osgi.jndi.service.name";

    // By default all local keys (without a dot) are forwarded to the DataSourceFactory.
    // These config keys will explicitly not be forwarded to the DataSourceFactory
    // (even though they are "local" keys without a dot ".")
    // Exception: for pooling support keys with prefix pool or factory are always
    // forwarded.
    private static final Set<String> NOT_FORWARDED_KEYS = new HashSet<>(Arrays.asList(
            ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME));
    private static final Set<String> FORWARDED_KEY_PREFIXES = new HashSet<>(Arrays.asList(
            "pool.", "factory."));
    // additionally all keys prefixed with "jms." will be forwarded (with the prefix stripped).
    private static final String CONFIG_KEY_PREFIX = "jms.";
    
    private static Logger LOG = LoggerFactory.getLogger(ConnectionFactoryRegistration.class);

    private TransactionManager transactionManager;
    private AutoCloseable connectionFactory;
    private ServiceRegistration<ConnectionFactory> serviceReg;

    public ConnectionFactoryRegistration(BundleContext context, ConnectionFactoryFactory cff, TransactionManager transactionManager, final Dictionary<String, Object> config, final Dictionary<String, Object> decryptedConfig) {
        this.transactionManager = transactionManager;
        String cfName = getCFName(config);
        if (cfName != null) {
            config.put(JNDI_SERVICE_NAME, cfName);
        }
        try {
            LOG.info("Found ConnectionFactoryFactory. Creating ConnectionFactory {}", cfName);
            ConnectionFactory cf = createCF(cff, decryptedConfig);
            if (cf instanceof AutoCloseable) {
                connectionFactory = (AutoCloseable) cf;
            }
            serviceReg = context.registerService(ConnectionFactory.class, cf, filterHidden(config));
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private static String getCFName(Dictionary<String, Object> config) {
        String jndiName = (String) config.get(ConnectionFactoryRegistration.JNDI_SERVICE_NAME);
        String cfName = (String) config.get(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME);
        if (cfName == null && jndiName == null) {
            throw new IllegalStateException("Can not determine ConnectionFactory name. Must set " + ConnectionFactoryRegistration.JNDI_SERVICE_NAME + " or " + ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME);
        }
        return jndiName != null ? jndiName : cfName;
    }

    @Override
    public void close() {
        if (serviceReg != null) {
            serviceReg.unregister();
        }
        safeClose(connectionFactory);
    }

    private ConnectionFactory createCF(ConnectionFactoryFactory cff, Dictionary<String, Object> decryptedConfig) throws Exception {
        Objects.requireNonNull(cff, "Must provide a ConnectionFactoryFactory");
        Map<String, Object> props = toMap(decryptedConfig);
        String user = (String) props.remove(ConnectionFactoryFactory.JMS_USER);
        String pswd = (String) props.remove(ConnectionFactoryFactory.JMS_PASSWORD);
        ConnectionFactory cf = cff.createConnectionFactory(props);
        XAConnectionFactory xaCf = cff.createXAConnectionFactory(props);
        ManagedConnectionFactoryBuilder builder = ManagedConnectionFactoryBuilder.builder()
                .transactionManager(transactionManager)
                .connectionFactory(cf, xaCf)
                .userName(user)
                .password(pswd);
        return builder.build();
    }

    private Map<String, Object> toMap(Dictionary<String, Object> dict) {
        Map<String, Object> props = new HashMap<>();
        Enumeration keys = dict.keys();
        while (keys.hasMoreElements()) {
            final String originalKey = (String) keys.nextElement();
            final String unhiddenKey = unhide(originalKey);
            if (shouldForwardToConnectionFactoryFactory(unhiddenKey)) {
                props.put(unhiddenKey, dict.get(originalKey));
            } else if (unhiddenKey.startsWith(CONFIG_KEY_PREFIX)) {
                props.put(unhiddenKey.substring(CONFIG_KEY_PREFIX.length()), dict.get(originalKey));
            }
        }
        return props;
    }

    private boolean shouldForwardToConnectionFactoryFactory(String key) {
        // only forward local configuration keys (i. e. those without a dot)
        // exception: the DATASOURCE_TYPE key (as legacy).
        boolean shouldForward = (!key.contains(".") && !NOT_FORWARDED_KEYS.contains(key));
        for (Iterator<String> it = FORWARDED_KEY_PREFIXES.iterator();
                !shouldForward && it.hasNext(); ) {
            shouldForward = key.startsWith(it.next());
        }
        return shouldForward;
    }

    private Dictionary filterHidden(Dictionary dict) {
        final Dictionary filtered = new Hashtable(dict.size());
        final Enumeration keys = dict.keys();
        while (keys.hasMoreElements()) {
            final String key = (String)keys.nextElement();
            if (!isHidden(key)) {
                filtered.put(key, dict.get(key));
            }
        }
        return filtered;
    }

    private String unhide(String key) {
        return isHidden(key) ? key.substring(1) : key;
    }

    private boolean isHidden(String key) {
        return key != null && key.startsWith(".");
    }

    private void safeClose(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.warn("Error closing " + closeable.getClass() + ": " + e.getMessage(), e);
            }
        }
    }

}
