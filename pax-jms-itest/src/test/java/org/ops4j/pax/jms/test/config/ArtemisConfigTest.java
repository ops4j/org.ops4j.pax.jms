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
package org.ops4j.pax.jms.test.config;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.jms.config.ConfigLoader;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.test.AbstractJmsTest;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * Uses the pax-jms-config module to create an Artemis ConnectionFactory from a configuration and validates the
 * ConnectionFactory is present as a service
 */
@RunWith(PaxExam.class)
public class ArtemisConfigTest extends AbstractJmsTest {

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
    public void testConnectionFactoryFromConfig() throws JMSException, IOException, InvalidSyntaxException, InterruptedException {
        org.osgi.service.cm.Configuration config = createConfigForConnectionFactory();
        ServiceTracker<ConnectionFactory, ConnectionFactory> tracker = new ServiceTracker<>(context, ConnectionFactory.class, null);
        tracker.open();
        ConnectionFactory cf = tracker.waitForService(2000);
        assertConnectionFactoryWorks(cf);
        assertServicePropertiesPresent(tracker.getServiceReference());
        checkConnectionFactoryIsDeletedWhenConfigIsDeleted(config, tracker);
        tracker.close();
    }

    @Test
    public void testConnectionFactoryFromEncryptedConfig() throws JMSException, IOException, InvalidSyntaxException, InterruptedException {
        StandardPBEStringEncryptor textEncryptor = new StandardPBEStringEncryptor();
        textEncryptor.setPassword("changeit!!!");
        textEncryptor.setKeyObtentionIterations(42);
        textEncryptor.setIvGenerator(new RandomIvGenerator());
        textEncryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_128");
        String encryptedUrl = textEncryptor.encrypt("tcp://localhost:61616");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("alias", "jasypt");
        context.registerService(StringEncryptor.class, textEncryptor, props);

        org.osgi.service.cm.Configuration config = createEncryptedConfigForConnectionFactory("ENC(" + encryptedUrl + ")");
        ServiceTracker<ConnectionFactory, ConnectionFactory> tracker = new ServiceTracker<>(context, ConnectionFactory.class, null);
        tracker.open();
        ConnectionFactory cf = tracker.waitForService(2000);
        assertConnectionFactoryWorks(cf);
        checkConnectionFactoryIsDeletedWhenConfigIsDeleted(config, tracker);
        tracker.close();
    }

    @Test
    public void testTwoConnectionFactorysFromConfig() throws Exception {
        org.osgi.service.cm.Configuration config1 = createConfigForConnectionFactory("cf1");
        org.osgi.service.cm.Configuration config2 = createConfigForConnectionFactory("cf2");
        ServiceTracker<ConnectionFactory, ConnectionFactory> tracker1 = new ServiceTracker<>(context, context.createFilter("(&(objectClass=" + ConnectionFactory.class.getName() + ")(osgi.jndi.service.name=cf1))"), null);
        ServiceTracker<ConnectionFactory, ConnectionFactory> tracker2 = new ServiceTracker<>(context, context.createFilter("(&(objectClass=" + ConnectionFactory.class.getName() + ")(osgi.jndi.service.name=cf2))"), null);
        tracker1.open();
        tracker2.open();
        ConnectionFactory cf1 = tracker1.waitForService(2000);
        ConnectionFactory cf2 = tracker2.waitForService(2000);
        assertConnectionFactoryWorks(cf1);
        assertConnectionFactoryWorks(cf2);
        assertServicePropertiesPresent(tracker1.getServiceReference(), "cf1");
        assertServicePropertiesPresent(tracker2.getServiceReference(), "cf2");

        FrameworkUtil.getBundle(ConfigLoader.class).stop();

        tracker1.close();
        tracker2.close();
    }

    private org.osgi.service.cm.Configuration createConfigForConnectionFactory() throws IOException {
        return createConfigForConnectionFactory("artemisTest");
    }

    private org.osgi.service.cm.Configuration createConfigForConnectionFactory(String jndiName) throws IOException {
        org.osgi.service.cm.Configuration config = configAdmin.createFactoryConfiguration("org.ops4j.connectionfactory", null);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        props.put(ConnectionFactoryFactory.JMS_URL, "tcp://localhost:61616");
        props.put(JNDI_NAME, jndiName); // jndi name for aries jndi
        config.update(props);
        return config;
    }

    private org.osgi.service.cm.Configuration createEncryptedConfigForConnectionFactory(String url) throws IOException {
        org.osgi.service.cm.Configuration config = configAdmin.createFactoryConfiguration("org.ops4j.connectionfactory", null);
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        props.put(ConnectionFactoryFactory.JMS_URL, url);
        props.put("decryptor", "jasypt");
        props.put(JNDI_NAME, "artemisEncryptedTest");
        config.update(props);
        return config;
    }

    private void assertConnectionFactoryWorks(ConnectionFactory cf) throws JMSException {
        assertNotNull("No ConnectionFactory service found", cf);
        cf.createConnection().close();
    }

    private void assertServicePropertiesPresent(ServiceReference<?> ref) {
        assertServicePropertiesPresent(ref, "artemisTest");
    }

    private void assertServicePropertiesPresent(ServiceReference<?> ref, String jndiName) {
        Assert.assertEquals("tcp://localhost:61616", ref.getProperty(ConnectionFactoryFactory.JMS_URL));
        Assert.assertEquals(jndiName, ref.getProperty(JNDI_NAME));
    }

    private void checkConnectionFactoryIsDeletedWhenConfigIsDeleted(
            org.osgi.service.cm.Configuration config, ServiceTracker<ConnectionFactory, ConnectionFactory> tracker)
            throws IOException, InterruptedException {
        config.delete();
        Thread.sleep(200);
        assertNull(tracker.getService());
    }

}
