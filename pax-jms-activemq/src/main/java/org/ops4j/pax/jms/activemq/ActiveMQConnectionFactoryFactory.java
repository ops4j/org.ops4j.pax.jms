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
package org.ops4j.pax.jms.activemq;

import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.ActiveMQXASslConnectionFactory;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import java.util.HashMap;
import java.util.Map;

public class ActiveMQConnectionFactoryFactory implements ConnectionFactoryFactory {

    @Override
    public ConnectionFactory createConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
        props = new HashMap<>(props);
        rename(props, ConnectionFactoryFactory.JMS_USER, "userName");
        String url = (String) props.remove(ConnectionFactoryFactory.JMS_URL);
        if (url == null) {
            throw new JMSRuntimeException("The url property must be set");
        }
        ActiveMQSslConnectionFactory cf = new ActiveMQSslConnectionFactory(url);
        cf.buildFromMap(props);
        // adapt ActiveMQ connection factory to JMS 2.0
        return new ConnectionFactory() {
            @Override
            public Connection createConnection() throws JMSException {
                return cf.createConnection();
            }

            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                return cf.createConnection(userName, password);
            }

            @Override
            public JMSContext createContext() {
                throw new UnsupportedOperationException("JMS 2.0 is not supported by ActiveMQ");
            }

            @Override
            public JMSContext createContext(int sessionMode) {
                throw new UnsupportedOperationException("JMS 2.0 is not supported by ActiveMQ");
            }

            @Override
            public JMSContext createContext(String userName, String password) {
                throw new UnsupportedOperationException("JMS 2.0 is not supported by ActiveMQ");
            }

            @Override
            public JMSContext createContext(String userName, String password, int sessionMode) {
                throw new UnsupportedOperationException("JMS 2.0 is not supported by ActiveMQ");
            }
        };
    }

    @Override
    public XAConnectionFactory createXAConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
        props = new HashMap<>(props);
        rename(props, ConnectionFactoryFactory.JMS_USER, "userName");
        String url = (String) props.remove(ConnectionFactoryFactory.JMS_URL);
        if (url == null) {
            throw new JMSRuntimeException("The url property must be set");
        }
        ActiveMQXASslConnectionFactory xaCf = new ActiveMQXASslConnectionFactory(url);
        xaCf.buildFromMap(props);
        // adapt ActiveMQ connection factory to JMS 2.0
        return new XAConnectionFactory() {
            @Override
            public XAConnection createXAConnection() throws JMSException {
                return xaCf.createXAConnection();
            }

            @Override
            public XAConnection createXAConnection(String userName, String password) throws JMSException {
                return xaCf.createXAConnection(userName, password);
            }

            @Override
            public XAJMSContext createXAContext() {
                throw new UnsupportedOperationException("JMS 2.0 is not supported by ActiveMQ");
            }

            @Override
            public XAJMSContext createXAContext(String userName, String password) {
                throw new UnsupportedOperationException("JMS 2.0 is not supported by ActiveMQ");
            }
        };
    }

    private void rename(Map<String, Object> props, String oldName, String newName) {
        Object t = props.remove(oldName);
        if (t != null) {
            props.put(newName, t);
        }
    }

}
