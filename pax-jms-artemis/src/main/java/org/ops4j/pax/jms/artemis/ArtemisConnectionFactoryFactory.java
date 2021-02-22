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
package org.ops4j.pax.jms.artemis;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.activemq.artemis.utils.uri.BeanSupport;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.util.PropertyUtil;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnectionFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ArtemisConnectionFactoryFactory implements ConnectionFactoryFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ArtemisConnectionFactoryFactory.class);

    @Override
    public ConnectionFactory createConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
        props = new HashMap<>(props);
        String url  = (String) props.remove(ConnectionFactoryFactory.JMS_URL);
        if (url == null) {
            throw new JMSRuntimeException("The url property must be set");
        }
        String protocol = (String) props.remove(ConnectionFactoryFactory.JMS_PROTOCOL);
        ConnectionFactory cf;
        if ("amqp".equalsIgnoreCase(protocol)) {
            cf = new JmsConnectionFactory(url);
            try {
                Properties properties = new Properties();
                properties.putAll(props);
                PropertyUtil.setProperties(cf, properties);
            } catch (Exception e) {
                throw (JMSRuntimeException) new JMSRuntimeException("Unable to build Artemis ConnectionFactory").initCause(e);
            }
        } else {
            cf = new ActiveMQConnectionFactory(url);
            try {
                BeanSupport.setData(cf, props);
            } catch (Exception e) {
                throw (JMSRuntimeException) new JMSRuntimeException("Unable to build Artemis ConnectionFactory").initCause(e);
            }
        }

        return cf;
    }

    @Override
    public XAConnectionFactory createXAConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
        props = new HashMap<>(props);
        String url  = (String) props.remove(ConnectionFactoryFactory.JMS_URL);
        if (url == null) {
            throw new JMSRuntimeException("The url property must be set");
        }
        String protocol = (String) props.remove(ConnectionFactoryFactory.JMS_PROTOCOL);
        if ("amqp".equalsIgnoreCase(protocol)) {
            LOG.warn("XAConnection is not supported when using the amqp protocol. Please check https://issues.apache.org/jira/projects/QPIDJMS/issues/QPIDJMS-206 for more information");
            return null;
        }
        ActiveMQXAConnectionFactory xaCf = new ActiveMQXAConnectionFactory(url);
        try {
            BeanSupport.setData(xaCf, props);
        } catch (Exception e) {
            throw (JMSRuntimeException) new JMSRuntimeException("Unable to build Artemis ConnectionFactory").initCause(e);
        }
        return xaCf;
    }

}
