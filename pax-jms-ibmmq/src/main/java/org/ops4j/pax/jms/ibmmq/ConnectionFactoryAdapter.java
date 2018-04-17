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
package org.ops4j.pax.jms.ibmmq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;

public class ConnectionFactoryAdapter implements ConnectionFactory, XAConnectionFactory {

    private ConnectionFactory targetConnectionFactory;
    private XAConnectionFactory xaTargetConnectionFactory;

    private interface Callable<V> {

        V call() throws JMSException;
    }

    private <V> V doCallInTccl(Callable<V> callable) throws JMSException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return callable.call();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private <V> V doCallInTcclRuntimeException(Callable<V> callable) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return callable.call();
        } catch (JMSException ex) {
            throw new JMSRuntimeException(ex.getMessage(), ex.getErrorCode(), ex);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    public ConnectionFactory getTargetConnectionFactory() {
        return targetConnectionFactory;
    }

    public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
        this.targetConnectionFactory = targetConnectionFactory;
    }

    public XAConnectionFactory getXaTargetConnectionFactory() {
        return xaTargetConnectionFactory;
    }

    public void setXaTargetConnectionFactory(XAConnectionFactory targetConnectionFactory) {
        this.xaTargetConnectionFactory = targetConnectionFactory;
        if (targetConnectionFactory instanceof ConnectionFactory) {
            setTargetConnectionFactory((ConnectionFactory) targetConnectionFactory);
        }
    }

    @Override
    public Connection createConnection() throws JMSException {
        return doCallInTccl(obtainTargetConnectionFactory()::createConnection);
    }

    @Override
    public Connection createConnection(String username, String password) throws JMSException {
        return doCallInTccl(() -> obtainTargetConnectionFactory().createConnection(username, password));
    }

    @Override
    public JMSContext createContext() {
        return doCallInTcclRuntimeException(obtainTargetConnectionFactory()::createContext);
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return doCallInTcclRuntimeException(() -> obtainTargetConnectionFactory().createContext(sessionMode));
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        return doCallInTcclRuntimeException(() -> obtainTargetConnectionFactory().createContext(userName, password));
    }

    @Override
    public JMSContext createContext(String userName, String password, int sessionMode) {
        return doCallInTcclRuntimeException(() -> obtainTargetConnectionFactory().createContext(userName, password, sessionMode));
    }

    @Override
    public XAConnection createXAConnection() throws JMSException {
        return doCallInTccl(obtainXaTargetConnectionFactory()::createXAConnection);
    }

    @Override
    public XAConnection createXAConnection(String username, String password) throws JMSException {
        return doCallInTccl(() -> obtainXaTargetConnectionFactory().createXAConnection(username, password));
    }

    @Override
    public XAJMSContext createXAContext() {
        return doCallInTcclRuntimeException(obtainXaTargetConnectionFactory()::createXAContext);
    }

    @Override
    public XAJMSContext createXAContext(String username, String password) {
        return doCallInTcclRuntimeException(() -> obtainXaTargetConnectionFactory().createXAContext(username, password));
    }

    private ConnectionFactory obtainTargetConnectionFactory() {
        return this.targetConnectionFactory;
    }

    private XAConnectionFactory obtainXaTargetConnectionFactory() {
        return this.xaTargetConnectionFactory;
    }
}
