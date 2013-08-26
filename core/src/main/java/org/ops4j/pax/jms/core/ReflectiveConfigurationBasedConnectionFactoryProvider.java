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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component provides access to a {@link ConnectionFactory} that is created
 * from an OSGi Configuration, it supports the following properties
 * <ul>
 * <li>org.ops4j.pax.jms.factoryclass - the fqn of the concrete
 * Connectionfactory class to use, it needs to have a defaultconstructor and a
 * package-export must exists for this</li>
 * <li>org.ops4j.pax.jms.property.[bean property name] - set the given property
 * on the ConnectionFactory after creation</li>
 * </ul>
 * 
 * @author Christoph Läubrich
 */
public class ReflectiveConfigurationBasedConnectionFactoryProvider implements ConnectionFactory {

    private static final Logger                      LOG                              = LoggerFactory.getLogger(ReflectiveConfigurationBasedConnectionFactoryProvider.class);

    private static final String                      PROPERTY_FACTORY_CLASS           = "org.ops4j.pax.jms.factoryclass";

    private static final String                      PROPERTY_FACTORY_PROPERTY_PREFIX = "org.ops4j.pax.jms.property.";

    private final AtomicReference<ConnectionFactory> connectionFactoryReference       = new AtomicReference<ConnectionFactory>();

    private Class<?>                                 factoryClass;

    public void activate(Map<String, ?> properties) throws IllegalArgumentException {
        String factoryClassProperty = (String) properties.get(PROPERTY_FACTORY_CLASS);
        if (factoryClassProperty == null) {
            throw logAndThrow(String.format("factory class property %s must be given", PROPERTY_FACTORY_CLASS));
        }
        try {
            factoryClass = getClass().getClassLoader().loadClass(factoryClassProperty);
        } catch (ClassNotFoundException e) {
            throw logAndThrow(String.format("factory class '%s' can't be loaded, make sure the package is exported by any bundle", factoryClassProperty), e);
        } catch (NoClassDefFoundError e) {
            throw logAndThrow(String.format("factory class '%s' can't be loaded, make sure providing bundle imports or embedds all dependencies", factoryClassProperty), e);
        }
        Object factoryInstanceObject;
        try {
            factoryInstanceObject = factoryClass.newInstance();
        } catch (InstantiationException e) {
            throw logAndThrow(String.format("factory class '%s' can't be instantiatied, make sure it has a public default constructor present", factoryClassProperty), e);
        } catch (IllegalAccessException e) {
            throw logAndThrow(String.format("factory class '%s' can't be instantiatied, make sure the class is public, has a public default constructor present and can be accessed", factoryClassProperty), e);
        } catch (RuntimeException e) {
            throw logAndThrow(String.format("factory class '%s' can't be instantiatied", factoryClassProperty), e);
        }
        final ConnectionFactory connectionFactory;
        if (factoryInstanceObject instanceof ConnectionFactory) {
            connectionFactory = (ConnectionFactory) factoryInstanceObject;
            configureFactory(connectionFactory, properties);
        } else {
            throw logAndThrow(String.format("factory class '%s' does not implement %s", factoryClassProperty, ConnectionFactory.class.getName()));
        }
        connectionFactoryReference.set(connectionFactory);
    }

    public void deactivate() {
        connectionFactoryReference.set(null);
    }

    @Override
    public Connection createConnection() throws JMSException {
        ConnectionFactory factory = connectionFactoryReference.get();
        if (factory == null) {
            throw new JMSException("ConnectionFactory disposed");
        }
        return factory.createConnection();
    }

    @Override
    public Connection createConnection(String username, String password) throws JMSException {
        ConnectionFactory factory = connectionFactoryReference.get();
        if (factory == null) {
            throw new JMSException("ConnectionFactory disposed");
        }
        return factory.createConnection(username, password);
    }

    @Override
    public String toString() {
        return "ConfigurationBasedConnectionFactory " + factoryClass;
    }

    /**
     * Configures the given factory using reflection
     * 
     * @param connectionFactory
     * @param properties
     * @throws ConfigurationException
     */
    private static void configureFactory(ConnectionFactory connectionFactory, Map<String, ?> properties) {
        Class<? extends ConnectionFactory> cfc = connectionFactory.getClass();
        Method[] methods = cfc.getMethods();
        entryLoop: for (Entry<String, ?> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith(PROPERTY_FACTORY_PROPERTY_PREFIX)) {
                String name = key.substring(PROPERTY_FACTORY_PROPERTY_PREFIX.length());
                for (Method method : methods) {
                    if (method.getName().equals("set" + name)) {
                        Class<?>[] types = method.getParameterTypes();
                        if (types.length == 1) {
                            Class<?> type = types[0];
                            try {
                                String valueOf = value == null ? "" : String.valueOf(value);
                                if (String.class.isAssignableFrom(type)) {
                                    method.invoke(connectionFactory, valueOf);
                                    continue entryLoop;
                                }
                                if (Boolean.class.isAssignableFrom(type)) {
                                    method.invoke(connectionFactory, Boolean.valueOf(valueOf));
                                    continue entryLoop;
                                }
                                if (Long.class.isAssignableFrom(type)) {
                                    method.invoke(connectionFactory, Long.valueOf(valueOf));
                                    continue entryLoop;
                                }
                                if (Integer.class.isAssignableFrom(type)) {
                                    method.invoke(connectionFactory, Integer.valueOf(valueOf));
                                    continue entryLoop;
                                }
                                if (Short.class.isAssignableFrom(type)) {
                                    method.invoke(connectionFactory, Short.valueOf(valueOf));
                                    continue entryLoop;
                                }
                            } catch (IllegalAccessException e) {
                                throw logAndThrow(String.format("can't access method %s for key %s", method, key), e);
                            } catch (InvocationTargetException e) {
                                throw logAndThrow(String.format("invoking method  %s for key %s failed", method, key), e);
                            } catch (RuntimeException e) {
                                throw logAndThrow(String.format("try to invoking method  %s for key %s failed", method, key), e);
                            }
                        }
                    }
                }
                throw logAndThrow(String.format("no suitable setter method found for 'set%s(...)', make sure it is public and accepts a String, boolean, long, integer or short as its only argument", name));
            }
        }
    }

    private static IllegalArgumentException logAndThrow(String format) {
        return logAndThrow(format, null);
    }

    private static IllegalArgumentException logAndThrow(String message, Throwable e) {
        if (e != null) {
            LOG.error(message, e);
        } else {
            LOG.error(message);
        }
        return new IllegalArgumentException(message, e);
    }
}
