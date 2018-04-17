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

import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnectionFactory;

import org.ops4j.pax.jms.service.ConnectionFactoryFactory;

public class MQConnectionFactoryFactory implements ConnectionFactoryFactory {

    private static final String IBM_MQ_CONNECTION_FACTORY_CLASS = "com.ibm.mq.jms.MQConnectionFactory";
    private static final String IBM_MQ_XA_CONNECTION_FACTORY_CLASS = "com.ibm.mq.jms.MQXAConnectionFactory";

    private final Class<?> ibmMqConnectionFactoryClass;
    private final Class<?> ibmMqXaConnectionFactoryClass;

    public MQConnectionFactoryFactory() throws ClassNotFoundException {
        ClassLoader classLoader = MQConnectionFactoryFactory.class.getClassLoader();
        this.ibmMqConnectionFactoryClass = classLoader.loadClass(IBM_MQ_CONNECTION_FACTORY_CLASS);
        this.ibmMqXaConnectionFactoryClass = classLoader.loadClass(IBM_MQ_XA_CONNECTION_FACTORY_CLASS);
    }

    @Override
    public ConnectionFactory createConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
        try {
            ConnectionFactory cf = ConnectionFactory.class.cast(ibmMqConnectionFactoryClass.newInstance());
            BeanConfig.configure(cf, props);
            ConnectionFactoryAdapter cfa = new ConnectionFactoryAdapter();
            cfa.setTargetConnectionFactory(cf);
            return cfa;
        } catch (Exception ex) {
            throw new JMSRuntimeException(ex.getMessage(), "", ex);
        }
    }

    @Override
    public XAConnectionFactory createXAConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
        try {
            XAConnectionFactory cf = XAConnectionFactory.class.cast(ibmMqXaConnectionFactoryClass.newInstance());
            BeanConfig.configure(cf, props);
            ConnectionFactoryAdapter cfa = new ConnectionFactoryAdapter();
            cfa.setXaTargetConnectionFactory(cf);
            return cfa;
        } catch (Exception ex) {
            throw new JMSRuntimeException(ex.getMessage(), "", ex);
        }
    }

    @SuppressWarnings("unused")
    private void setProperties(Object cf, Class<?> clazz, Map<String, Object> props) throws Exception {

        String hostName = (String) props.remove("hostName");
        clazz.getMethod("setHostName", String.class).invoke(cf, hostName);

        String port = (String) props.remove("port");
        if (port != null) {
            int portNum = Integer.parseInt(port);
            clazz.getMethod("setPort", int.class).invoke(cf, portNum);
        }

        String queueManager = (String) props.remove("queueManager");
        clazz.getMethod("setQueueManager", String.class).invoke(cf, queueManager);

        String channel = (String) props.remove("channel");
        if (channel != null) {
            clazz.getMethod("setChannel", String.class).invoke(cf, channel);
        }

        String transportType = (String) props.remove("transportType");
        if (transportType != null) {
            int transportTypeNum = Integer.parseInt(transportType);
            clazz.getMethod("setTransportType", int.class).invoke(cf, transportTypeNum);
        }
    }

}
