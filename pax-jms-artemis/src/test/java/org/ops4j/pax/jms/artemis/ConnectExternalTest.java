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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
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
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConnectExternalTest {

    public static final Logger LOG = LoggerFactory.getLogger(ConnectExternalTest.class);

    /*
        We can test with:
         - apache-artemis-2.19.1-bin.zip
         - apache-artemis-2.29.0-bin.zip (requires Java 11)

        $ pwd
        /data/servers/apache-artemis-2.19.1
        $ bin/artemis create --user paxjms --password paxjms --require-login paxjms
        Creating ActiveMQ Artemis instance at: /data/servers/apache-artemis-2.19.1/paxjms

        Auto tuning journal ...
        done! Your system can make 27.78 writes per millisecond, your journal-buffer-timeout will be 36000

        You can now start the broker by executing:

           "/data/servers/apache-artemis-2.19.1/paxjms/bin/artemis" run

        Or you can run the broker in the background using:

           "/data/servers/apache-artemis-2.19.1/paxjms/bin/artemis-service" start

        $ paxjms/bin/artemis run
             _        _               _
            / \  ____| |_  ___ __  __(_) _____
           / _ \|  _ \ __|/ _ \  \/  | |/  __/
          / ___ \ | \/ |_/  __/ |\/| | |\___ \
         /_/   \_\|   \__\____|_|  |_|_|/___ /
         Apache ActiveMQ Artemis 2.19.1


        Jul 26, 2023 10:03:10 AM java.lang.System$LoggerFinder lambda$accessProvider$0
        ...
        $ # run the test
        $ ^C

        $ pwd
        /data/servers/apache-artemis-2.29.0
        Creating ActiveMQ Artemis instance at: /data/servers/apache-artemis-2.29.0/paxjms

        Auto tuning journal ...
        done! Your system can make 27.78 writes per millisecond, your journal-buffer-timeout will be 36000

        You can now start the broker by executing:

           "/data/servers/apache-artemis-2.29.0/paxjms/bin/artemis" run

        Or you can run the broker in the background using:

           "/data/servers/apache-artemis-2.29.0/paxjms/bin/artemis-service" start

        $ paxjms/bin/artemis run
             _        _               _
            / \  ____| |_  ___ __  __(_) _____
           / _ \|  _ \ __|/ _ \  \/  | |/  __/
          / ___ \ | \/ |_/  __/ |\/| | |\___ \
         /_/   \_\|   \__\____|_|  |_|_|/___ /
         Apache ActiveMQ Artemis 2.29.0


        2023-07-26 10:06:34,804 INFO  [org.apache.activemq.artemis.integration.bootstrap] AMQ101000: Starting ActiveMQ Artemis Server version 2.29.0
        ...
        $ # run the test
        $ ^C
     */

    @Rule
    public ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() {
            try (Socket socket = new Socket()) {
                InetSocketAddress endpoint = new InetSocketAddress("localhost", 61616);
                socket.connect(endpoint, (int) TimeUnit.SECONDS.toMillis(5));
                Assume.assumeTrue(true);
            } catch (Exception ex) {
                Assume.assumeTrue(false);
            }
        }
    };

    @Test
    public void jmsConnect() throws Exception {
        ConnectionFactory cf = new ActiveMQQueueConnectionFactory("tcp://127.0.0.1:61616");
        ConnectionMetaData md;
        try (Connection con = cf.createConnection("paxjms", "paxjms")) {
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
        try (JMSContext ctx = cf.createContext("paxjms", "paxjms")) {
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
        try (Connection con = ff.createConnectionFactory(props).createConnection("paxjms", "paxjms")) {
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
