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

import org.ops4j.pax.jms.config.ConfigLoader;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    private static final String FACTORY_PID = "org.ops4j.connectionfactory";

    private ServiceTracker<?, ?> connectionFactoryTracker;

    private ExternalConfigLoader externalConfigLoader;
    private ServiceRegistration<ConfigLoader> configLoaderRegistration;
    private ConnectionFactoryConfigManager configManager;
    private ServiceRegistration<ManagedServiceFactory> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        configLoaderRegistration = context.registerService(ConfigLoader.class, new FileConfigLoader(), new Hashtable<>());
        externalConfigLoader = new ExternalConfigLoader(context);
        Dictionary<String, String> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, FACTORY_PID);
        configManager = new ConnectionFactoryConfigManager(context, externalConfigLoader);
        // this service will track:
        //  - org.ops4j.connectionfactory factory PIDs
        //  - (optionally) org.jasypt.encryption.StringEncryptor services
        //  - org.ops4j.pax.jms.service.ConnectionFactoryFactory services
        registration = context.registerService(ManagedServiceFactory.class, configManager, props);

        // this service will track:
        //  - javax.jms.ConnectionFactory services
        //  - javax.jms.XAConnectionFactory services
        // and when they're registered:
        //  - with "pool=<pool name>"
        //  - without "pax.jms.managed=true"
        // they'll be processed by selected org.ops4j.pax.jms.service.PooledConnectionFactoryFactory
        // (as with org.ops4j.connectionfactory factory PIDs)
        ServiceTrackerHelper helper = ServiceTrackerHelper.helper(context);
        String filter = "(&(pool=*)(!(pax.jms.managed=true))" +
                "(|(objectClass=javax.jms.ConnectionFactory)(objectClass=javax.jms.XAConnectionFactory)))";
        connectionFactoryTracker = helper.track(Object.class, filter,
                (cf, reference) -> new ConnectionFactoryWrapper(context, externalConfigLoader, cf, reference),
                ConnectionFactoryWrapper::close
        );
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (connectionFactoryTracker != null) {
            connectionFactoryTracker.close();
        }
        registration.unregister();
        configManager.destroy();
        configLoaderRegistration.unregister();
        externalConfigLoader.destroy();
    }

}
