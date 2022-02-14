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
package org.ops4j.pax.jms.test.artemis;

import java.io.File;
import java.util.HashMap;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionMetaData;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.jms.artemis.ArtemisConnectionFactoryFactory;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.test.AbstractJmsTest;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Uses the pax-jms-config module to create an Artemis ConnectionFactory from a configuration and validates the
 * ConnectionFactory is present as a service
 */
@RunWith(PaxExam.class)
public class ArtemisConnectionTest extends AbstractJmsTest {

    private static final String JNDI_NAME = "osgi.jndi.service.name";

    @Inject
    ConfigurationAdmin configAdmin;

    private EmbeddedActiveMQ broker;

    @Configuration
    public Option[] config() {
        return combine(
                baseConfiguration(),
                mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jasypt"),
                mvnBundle("org.ops4j.pax.jms", "pax-jms-api"),
                mvnBundle("org.ops4j.pax.jms", "pax-jms-config"),
                mvnBundle("org.ops4j.pax.jms", "pax-jms-artemis"),
                // we have to install all bundles required by artemis-core-client and artemis-jms-client features
                mvnBundle("javax.jms", "javax.jms-api"),
                // there's a little problem with this bundle conflicting with artemis-server-osgi - but that's
                // only in client + server scenario...
                mvnBundle("org.apache.activemq", "artemis-jms-client-osgi"),
                mvnBundle("org.apache.activemq", "artemis-core-client-osgi"),
                mvnBundle("org.apache.activemq", "activemq-artemis-native"),
                // bundles needed to start embedded Artemis broker
                mvnBundle("org.osgi", "org.osgi.service.component"),
                mvnBundle("org.osgi", "org.osgi.util.function"),
                mvnBundle("org.osgi", "org.osgi.util.promise"),
                mvnBundle("org.apache.felix", "org.apache.felix.scr"),
                mvnBundle("org.apache.commons", "commons-configuration2"),
                mvnBundle("org.apache.commons", "commons-lang3"),
                mvnBundle("org.apache.commons", "commons-text"),
                mvnBundle("commons-beanutils", "commons-beanutils"),
                mvnBundle("commons-collections", "commons-collections"),
                mvnBundle("org.jgroups", "jgroups"),
                mvnBundle("org.jctools", "jctools-core"),
                mvnBundle("org.apache.johnzon", "johnzon-core"),
                mvnBundle("com.google.guava", "guava"),
                mvnBundle("com.google.guava", "failureaccess"),
                mvnBundle("javax.json", "javax.json-api"),
                mvnBundle("javax.mail", "javax.mail-api"),
                mvnBundle("com.sun.activation", "javax.activation"),
                mvnBundle("com.sun.mail", "javax.mail"),
                mvnBundle("io.netty", "netty-buffer"),
                mvnBundle("io.netty", "netty-codec"),
                mvnBundle("io.netty", "netty-codec-http"),
                mvnBundle("io.netty", "netty-codec-socks"),
                mvnBundle("io.netty", "netty-common"),
                mvnBundle("io.netty", "netty-handler"),
                mvnBundle("io.netty", "netty-handler-proxy"),
                mvnBundle("io.netty", "netty-resolver"),
                mvnBundle("io.netty", "netty-transport"),
                mvnBundle("io.netty", "netty-tcnative-classes"),
                mavenBundle("io.netty", "netty-transport-classes-epoll").versionAsInProject(),
                mavenBundle("io.netty", "netty-transport-native-epoll").classifier("linux-x86_64").versionAsInProject().noStart(),
                mvnBundle("io.netty", "netty-transport-native-unix-common"),
                mvnBundle("io.netty", "netty-transport-classes-kqueue"),
                mvnBundle("io.netty", "netty-transport-native-kqueue"),
                mvnBundle("org.apache.activemq", "artemis-quorum-api"),
                mvnBundle("org.apache.activemq", "artemis-server-osgi")
        );
    }

    @Before
    public void setupBroker() throws Exception {
        FileDeploymentManager deploymentManager
                = new FileDeploymentManager(new File("target/test-classes/test-broker.xml").getAbsoluteFile().toURI().toURL().toString());
        FileConfiguration config = new FileConfiguration();
        deploymentManager.addDeployable(config);
        deploymentManager.readConfiguration();
        broker = new EmbeddedActiveMQ().setConfiguration(config);
        broker.start();
    }

    @After
    public void stopBroker() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(broker.getClass().getClassLoader());
            broker.stop();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    @Test
    public void testConnectionUsingJmsApi() throws Exception {
        ConnectionFactoryFactory ff = new ArtemisConnectionFactoryFactory();
        HashMap<String, Object> props = new HashMap<>();
        props.put(ConnectionFactoryFactory.JMS_URL, "tcp://127.0.0.1:61616");
        ConnectionMetaData md;
        try (Connection con = ff.createConnectionFactory(props).createConnection()) {
            con.start();
            md = con.getMetaData();
            LOG.info("{}/{}", md.getJMSProviderName(), md.getProviderVersion());

            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Queue dest = session.createQueue("q1");
                try (MessageProducer producer = session.createProducer(dest)) {
                    TextMessage tm = session.createTextMessage("Hello!");
                    producer.send(tm);
                }
            }

            try (Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Queue dest = session.createQueue("q1");
                try (MessageConsumer consumer = session.createConsumer(dest)) {
                    TextMessage tm = (TextMessage) consumer.receive();
                    assertThat(tm.getText(), equalTo("Hello!"));
                }
            }
        }
    }

}
