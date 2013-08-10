/*
 * #%L
 * OPS4J Pax JMS core bundle
 * %%
 * Copyright (C) 2013 OPS4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * A wrapper that blocks several method calls
 * 
 * @author Christoph Läubrich
 */
public class DelegateConnection implements Connection {

    private final Connection delegate;

    public DelegateConnection(Connection delegate) {
        this.delegate = delegate;
    }

    /**
     * @return the current value of delegate
     */
    Connection getDelegate() {
        return delegate;
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector, ServerSessionPool sessionPool, int maxMessages)
            throws JMSException {
        return delegate.createConnectionConsumer(destination, messageSelector, sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName, String messageSelector, ServerSessionPool sessionPool,
            int maxMessages) throws JMSException {
        return delegate.createDurableConnectionConsumer(topic, subscriptionName, messageSelector, sessionPool, maxMessages);
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
        return delegate.createSession(transacted, acknowledgeMode);
    }

    @Override
    public String getClientID() throws JMSException {
        return delegate.getClientID();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return delegate.getExceptionListener();
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return delegate.getMetaData();
    }

    /**
     * @throws JMSException
     *             always
     */
    @Override
    public void close() throws JMSException {
        throw new JMSException("Sorry you can't close this connection");
    }

    /**
     * @throws JMSException
     *             always
     */
    @Override
    public void setClientID(String clientID) throws JMSException {
        throw new JMSException("Sorry you can't set the client ID on this connection");
    }

    /**
     * @throws JMSException
     *             always
     */
    @Override
    public void setExceptionListener(ExceptionListener listener) throws JMSException {
        throw new JMSException("Sorry you can't set the exception listener on this connection, considder register it as a service in the OSGi service registry");
    }

    /**
     * @throws JMSException
     *             always
     */
    @Override
    public void start() throws JMSException {
        throw new JMSException("Sorry you can't start this connection");
    }

    /**
     * @throws JMSException
     *             always
     */
    @Override
    public void stop() throws JMSException {
        throw new JMSException("Sorry you can't stop this connection");
    }
}
