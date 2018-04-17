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
package org.ops4j.pax.jms.pool.narayana;

import java.util.Dictionary;
import java.util.Hashtable;
import javax.transaction.TransactionManager;

import org.ops4j.pax.jms.service.PooledConnectionFactoryFactory;
import org.ops4j.pax.jms.service.internal.AbstractTransactionManagerTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.ops4j.pax.jms.service.PooledConnectionFactoryFactory.POOL_KEY;
import static org.ops4j.pax.jms.service.PooledConnectionFactoryFactory.XA_KEY;

public class Activator implements BundleActivator {

    private static final String NARAYANA = "narayana";
    private AbstractTransactionManagerTracker<TransactionManager> tmTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        PooledJmsPooledConnectionFactoryFactory cff = new PooledJmsPooledConnectionFactoryFactory();
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(POOL_KEY, NARAYANA);
        props.put(XA_KEY, "false");
        context.registerService(PooledConnectionFactoryFactory.class, cff, props);

        tmTracker = new AbstractTransactionManagerTracker<TransactionManager>(context, TransactionManager.class) {
            @Override
            public ServiceRegistration<PooledConnectionFactoryFactory> createService(BundleContext context,
                                                                                     TransactionManager tm) {
                PooledJmsXAPooledConnectionFactoryFactory cff = new PooledJmsXAPooledConnectionFactoryFactory(context, tm);
                Dictionary<String, String> props = new Hashtable<String, String>();
                props.put(POOL_KEY, NARAYANA);
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
