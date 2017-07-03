/**
 * Copyright 2013 OPS4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.jms.core;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The connectionprovider uses a {@link ConnectionFactory} and provides a
 * {@link Connection} as a service to other bundles. A
 * {@link JMSConnectionProvider} is (as mentioned in the spec) threadsafe and
 * normaly there is no need for more than one connection.
 * 
 * @author Christoph Läubrich
 */
public class JMSConnectionProvider implements ExceptionListener {

    private static final String              PROPERTY_JMS_PASSWORD = "org.ops4j.pax.jms.password";

    private static final String              PROPERTY_JMS_USERNAME = "org.ops4j.pax.jms.username";

    private static final Logger              LOG                   = LoggerFactory.getLogger(JMSConnectionProvider.class);

    private final ExecutorService            executor              = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private ConnectionFactory                connectionFactory;

    private String                           username;

    private String                           password;

    private String                           clientid;

    private final Dictionary<String, Object> serviceProperties     = new Hashtable<String, Object>();

    private ConnectionRegistration           connectionRegistration;

    private BundleContext                    bundleContext;

    private boolean                          stopped;

    private Long                             id;

    public void activate(Map<String, ?> properties, BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        username = (String) properties.get(PROPERTY_JMS_USERNAME);
        password = (String) properties.get(PROPERTY_JMS_PASSWORD);
        clientid = (String) properties.get("org.ops4j.pax.jms.clientid");
        id = (Long) properties.get("component.id");
        for (Entry<String, ?> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(".") || key.equals(PROPERTY_JMS_PASSWORD) || key.equals(PROPERTY_JMS_USERNAME)) {
                //don't propagate private properties...
                continue;
            }
            serviceProperties.put(key, entry.getValue());
        }
        onException(null);
    }

    public void deactivate() {
        synchronized (JMSConnectionProvider.this) {
            stopped = true;
            if (connectionRegistration != null) {
                connectionRegistration.unregister();
                connectionRegistration = null;
            }
        }
        executor.shutdown();
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void onException(JMSException exception) {
        synchronized (JMSConnectionProvider.this) {
            if (connectionRegistration != null) {
                connectionRegistration.unregister();
                connectionRegistration = null;
            }
            if (stopped) {
                //if we are already stopped... no further actions should be taken...
                return;
            }
            if (exception != null) {
                LOG.error("[ID {}]  JMS provider detects a serious problem with the connection, shutting down service and try to aquire a new one", id, exception);
            }
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    Thread thread = Thread.currentThread();
                    String oldName = thread.getName();
                    try {
                        thread.setName("JMSConnectionProvider-Connection-Establish-Thread-" + id);
                        LOG.info("[ID {}]  Try to establish connection to JMS provider {}", id, connectionFactory);
                        while (!thread.isInterrupted() && !stopped) {
                            try {
                                final Connection connection;
                                if (username != null || password != null) {
                                    connection = connectionFactory.createConnection(username, password);
                                } else {
                                    connection = connectionFactory.createConnection();
                                }
                                if (clientid != null) {
                                    connection.setClientID(clientid);
                                }
                                connection.setExceptionListener(JMSConnectionProvider.this);
                                ConnectionMetaData metaData = connection.getMetaData();
                                ConnectionRegistration registration = new ConnectionRegistration(connection);
                                LOG.info("[ID {}]  JMS connection created for {} version {} and JMS {}", new Object[] { id, metaData.getJMSProviderName(),
                                        metaData.getProviderVersion(), metaData.getJMSVersion() });
                                synchronized (JMSConnectionProvider.this) {
                                    if (!stopped) {
                                        connectionRegistration = registration;
                                        registration.register();
                                    } else {
                                        registration.unregister();
                                    }
                                }
                                Executors.defaultThreadFactory().newThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Thread.currentThread().setName("JMSConnectionProvider-Starting-Connection-Thread-" + id);
                                        LOG.debug("[ID {}]  Starting connection...", id);
                                        try {
                                            connection.start();
                                            LOG.debug("[ID {}]  Connection started!", id);
                                        } catch (JMSException e) {
                                            //If start failed, we start over again...
                                            onException(e);
                                        }
                                    }
                                }).start();
                                return;
                            } catch (RuntimeException e) {
                                LOG.debug("[ID {}]  Unknown problem while creating connection", id, e);
                            } catch (JMSException e) {
                                LOG.debug("[ID {}]  Creating connection failed", id, e);
                            }
                            //We wait some time before the next attempt
                            try {
                                TimeUnit.SECONDS.sleep(2);
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                    } finally {
                        thread.setName(oldName);
                    }
                }
            });
        }
    }

    private class ConnectionRegistration {

        private final DelegateConnection        connection;
        private ServiceRegistration<Connection> registerService;

        public ConnectionRegistration(Connection connection) {
            this.connection = new DelegateConnection(connection);
        }

        public void register() {
            registerService = bundleContext.registerService(Connection.class, connection, serviceProperties);
        }

        public void unregister() {
            if (registerService != null) {
                registerService.unregister();
            }
            //A stop can take some time... we dispatch this into a seperate thread
            Executors.defaultThreadFactory().newThread(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName("JMSConnectionProvider-Stopping-Connection-Thread-" + id);
                    LOG.debug("[ID {}]  Stopping connection...", id);
                    try {
                        connection.getDelegate().stop();
                    } catch (JMSException e) {
                        //If stop fails, we don't care...
                    }
                    LOG.debug("[ID {}]  Closing connection...", id);
                    try {
                        connection.getDelegate().close();
                    } catch (JMSException e) {
                        //If stop fails, we don't care...
                    }
                    LOG.debug("[ID {}]  Connection closed!", id);
                }
            }).start();
        }
    }

}
