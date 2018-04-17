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

import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.jboss.tm.XAResourceRecovery;
import org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.service.internal.BeanConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.jms.service.internal.BeanConfig.getNonPoolProps;
import static org.ops4j.pax.jms.service.internal.BeanConfig.getPoolProps;

public class PooledJmsXAPooledConnectionFactoryFactory extends PooledJmsPooledConnectionFactoryFactory {

    public static final Logger LOG = LoggerFactory.getLogger(PooledJmsXAPooledConnectionFactoryFactory.class);

    private final TransactionManager transactionManager;
    private final BundleContext bundleContext;

    private ServiceRegistration<XAResourceRecovery> recovery;

    public PooledJmsXAPooledConnectionFactoryFactory(BundleContext bundleContext, TransactionManager transactionManager) {
        this.bundleContext = bundleContext;
        this.transactionManager = transactionManager;
    }

    @Override
    public ConnectionFactory create(ConnectionFactoryFactory cff, Map<String, Object> props) throws JMSRuntimeException {
        try {
            XAConnectionFactory cf = cff.createXAConnectionFactory(getNonPoolProps(props));
            JmsPoolXAConnectionFactory pool = new JmsPoolXAConnectionFactory();
            pool.setConnectionFactory(cf);
            pool.setTransactionManager(transactionManager);
            BeanConfig.configure(pool, getPoolProps(props));

            // this service will be unregistered when pax-jms-pool-narayana will be unregistered
            recovery = bundleContext.registerService(XAResourceRecovery.class, () -> {
                try {
                    return new XAResource[] { new Wrapper(cf.createXAConnection()) };
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }, null);

            return new CloseableConnectionFactory(pool) {
                @Override
                public void close() throws Exception {
                    recovery.unregister();
                }
            };
        } catch (Throwable e) {
            LOG.error("Error creating pooled connection factory: " + e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    class Wrapper implements XAResource {

        private final XAConnection xaConnection;
        private final XAResource xaResource;

        Wrapper(XAConnection xaConnection) throws JMSException {
            this.xaConnection = xaConnection;
            this.xaResource = xaConnection.createXASession().getXAResource();
        }

        @Override
        public void commit(Xid xid, boolean b) throws XAException {
            xaResource.commit(xid, b);
        }

        @Override
        public void end(Xid xid, int i) throws XAException {
            xaResource.end(xid, i);
        }

        @Override
        public void forget(Xid xid) throws XAException {
            xaResource.forget(xid);
        }

        @Override
        public int getTransactionTimeout() throws XAException {
            return xaResource.getTransactionTimeout();
        }

        @Override
        public boolean isSameRM(XAResource xaResource) throws XAException {
            return this.xaResource.isSameRM(xaResource);
        }

        @Override
        public int prepare(Xid xid) throws XAException {
            return xaResource.prepare(xid);
        }

        @Override
        public Xid[] recover(int i) throws XAException {
            if (i == TMENDRSCAN) {
                try {
                    xaConnection.close();
                    return null;
                } catch (JMSException e) {
                    throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
                }
            } else {
                return xaResource.recover(i);
            }
        }

        @Override
        public void rollback(Xid xid) throws XAException {
            xaResource.rollback(xid);
        }

        @Override
        public boolean setTransactionTimeout(int i) throws XAException {
            return xaResource.setTransactionTimeout(i);
        }

        @Override
        public void start(Xid xid, int i) throws XAException {
            xaResource.start(xid, i);
        }
    }

}
