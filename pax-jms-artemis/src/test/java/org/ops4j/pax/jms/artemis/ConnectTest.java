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

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQQueueConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConnectTest {

    public static final Logger LOG = LoggerFactory.getLogger(ConnectTest.class);

    @Rule
    public EmbeddedActiveMQResource resource = new EmbeddedActiveMQResource(new File("target/test-classes/test-broker.xml").getAbsoluteFile().toURI().toURL().toString());

    public ConnectTest() throws MalformedURLException {
    }

    @Test
    public void jmsConnect() throws Exception {
        ConnectionFactory cf = new ActiveMQQueueConnectionFactory("tcp://127.0.0.1:61616");
        ConnectionMetaData md;
        try (Connection con = cf.createConnection()) {
            con.start();
            md = con.getMetaData();
            LOG.info("{}/{}", md.getJMSProviderName(), md.getProviderVersion());

            ActiveMQQueue dest = new ActiveMQQueue("q1");

            try (Session session = con.createSession()) {
                try (MessageProducer producer = session.createProducer(dest)) {
                    TextMessage tm = session.createTextMessage("Hello!");
                    producer.send(tm);
                }
            }
        }
        // JMS 2.0 API
        try (JMSContext ctx = cf.createContext()) {
            ctx.start();
            ActiveMQQueue dest = new ActiveMQQueue("q1");

            try (JMSConsumer consumer = ctx.createConsumer(dest)) {
                TextMessage tm = (TextMessage) consumer.receive();
                assertThat(tm.getText(), equalTo("Hello!"));
            }
        }
    }

    @Test
    public void paxJmsConnect() throws Exception {
        ConnectionFactoryFactory ff = new ArtemisConnectionFactoryFactory();
        HashMap<String, Object> props = new HashMap<>();
        props.put(ConnectionFactoryFactory.JMS_URL, "tcp://127.0.0.1:61616");
        ConnectionMetaData md;
        try (Connection con = ff.createConnectionFactory(props).createConnection()) {
            con.start();
            md = con.getMetaData();
            LOG.info("{}/{}", md.getJMSProviderName(), md.getProviderVersion());

            ActiveMQQueue dest = new ActiveMQQueue("q1");

            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try (MessageProducer producer = session.createProducer(dest)) {
                    TextMessage tm = session.createTextMessage("Hello!");
                    producer.send(tm);
                }
            }

            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                try (MessageConsumer consumer = session.createConsumer(dest)) {
                    TextMessage tm = (TextMessage) consumer.receive();
                    assertThat(tm.getText(), equalTo("Hello!"));
                }
            }
        }
    }

}
