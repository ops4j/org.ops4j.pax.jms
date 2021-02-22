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

import java.util.HashMap;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConnectTest {

    public static final Logger LOG = LoggerFactory.getLogger(ConnectTest.class);

    private BrokerService broker;

    @Before
    public void before() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.addConnector("tcp://0.0.0.0:61616");
        broker.start();
    }

    @After
    public void after() throws Exception {
        broker.stop();
    }

    @Test
    public void jmsConnect() throws Exception {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://0.0.0.0:61616");
        ConnectionMetaData md;
        try (Connection con = cf.createConnection()) {
            con.start();
            md = con.getMetaData();
            LOG.info("{}/{}", md.getJMSProviderName(), md.getProviderVersion());

            ActiveMQQueue dest = new ActiveMQQueue("q1");

            // can use only JMS 1.1 API
            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try (MessageProducer producer = session.createProducer(dest)) {
                    TextMessage tm = session.createTextMessage("Hello!");
                    producer.send(tm);
                }
            }
        }
        try (Connection con = cf.createConnection()) {
            con.start();
            ActiveMQQueue dest = new ActiveMQQueue("q1");

            // can use only JMS 1.1 API
            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try (MessageConsumer consumer = session.createConsumer(dest)) {
                    TextMessage tm = (TextMessage) consumer.receive();
                    assertThat(tm.getText(), equalTo("Hello!"));
                }
            }
        }
    }

    @Test
    public void paxJmsConnect() throws Exception {
        ConnectionFactoryFactory ff = new ActiveMQConnectionFactoryFactory();
        HashMap<String, Object> props = new HashMap<>();
        props.put(ConnectionFactoryFactory.JMS_URL, "tcp://0.0.0.0:61616");
        ConnectionMetaData md;
        try (Connection con = ff.createConnectionFactory(props).createConnection()) {
            con.start();
            md = con.getMetaData();
            LOG.info("{}/{}", md.getJMSProviderName(), md.getProviderVersion());

            ActiveMQQueue dest = new ActiveMQQueue("q1");

            // can use only JMS 1.1 API
            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try (MessageProducer producer = session.createProducer(dest)) {
                    TextMessage tm = session.createTextMessage("Hello!");
                    producer.send(tm);
                }
            }

            // can use only JMS 1.1 API
            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try (MessageConsumer consumer = session.createConsumer(dest)) {
                    TextMessage tm = (TextMessage) consumer.receive();
                    assertThat(tm.getText(), equalTo("Hello!"));
                }
            }
        }
    }

}
