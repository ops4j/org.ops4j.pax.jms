/*
 * Copyright 2021 OPS4J.
 *
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
 */
package org.ops4j.pax.jms.pool.transx;

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.jms.service.PooledConnectionFactoryFactory;
import org.ops4j.pax.jms.service.internal.AbstractTransactionManagerTracker;
import org.ops4j.pax.transx.tm.TransactionManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.ops4j.pax.jms.service.PooledConnectionFactoryFactory.POOL_KEY;
import static org.ops4j.pax.jms.service.PooledConnectionFactoryFactory.XA_KEY;

public class Activator implements BundleActivator {

    private static final String TRANSX = "transx";
    private AbstractTransactionManagerTracker<TransactionManager> tmTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        TransxPooledConnectionFactoryFactory cff = new TransxPooledConnectionFactoryFactory();
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(POOL_KEY, TRANSX);
        props.put(XA_KEY, "false");
        context.registerService(PooledConnectionFactoryFactory.class, cff, props);

        tmTracker = new AbstractTransactionManagerTracker<TransactionManager>(context, TransactionManager.class) {
            @Override
            public ServiceRegistration<PooledConnectionFactoryFactory> createService(BundleContext context,
                                                                                     TransactionManager tm) {
                TransxXAPooledConnectionFactoryFactory cff = new TransxXAPooledConnectionFactoryFactory(tm);
                Dictionary<String, String> props = new Hashtable<String, String>();
                props.put(POOL_KEY, TRANSX);
                props.put(XA_KEY, "true");
                return context.registerService(PooledConnectionFactoryFactory.class, cff, props);
            }
        };

        tmTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tmTracker.close();
    }

}
