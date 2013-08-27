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
package org.ops4j.pax.jms.itest;

import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.cm.ConfigurationAdminOptions;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base for all itest, setting up the Pax JMS core for test
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITestBase {

    protected final Logger      LOG = LoggerFactory.getLogger(getClass());

    /**
     * The base Object for a {@link Connection}
     */
    @Inject
    @Filter(timeout = 60000)
    protected Connection        connection;

    /**
     * The base Object for a {@link ConnectionFactory}
     */
    @Inject
    @Filter(timeout = 60000)
    protected ConnectionFactory factory;

    @Inject
    protected BundleContext     bundleContext;

    @Configuration
    public Option[] configure() {
        return CoreOptions.options(CoreOptions.junitBundles(),
        //JMS spec
        CoreOptions.mavenBundle("org.apache.geronimo.specs", "geronimo-jms_1.1_spec", "1.1.1"),
        // We require DS
        CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.6.2").startLevel(1).start(true),
        // and we want CM...
        CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.6.0").startLevel(1).start(true),
        //Active MQ as JMS provider, wrapped as bundle
        CoreOptions.wrappedBundle(CoreOptions.mavenBundle("org.apache.activemq", "activemq-all", "5.8.0"))
        // - export the relevant package
        .exports("org.apache.activemq")
        // - and import some we require from external...
        .imports("javax.jms", "javax.naming", "javax.net", "javax.management", "javax.management.loading", "javax.management.modelmbean", "javax.management.monitor", "javax.management.openmbean", "javax.management.relation", "javax.management.remote", "javax.management.remote.rmi", "javax.management.timer").start(true),
        //Pax JMS core
        CoreOptions.mavenBundle("org.ops4j.pax.jms", "org.ops4j.pax.jms.core", "0.0.1-SNAPSHOT"),
        // Add Configuration to setup ActiveMQ
        ConfigurationAdminOptions.newConfiguration("org.ops4j.pax.jms.ConfigurationBasedConnectionFactoryProvider")
        // - Configure factory to use
        .put("org.ops4j.pax.jms.factoryclass", "org.apache.activemq.ActiveMQConnectionFactory")
        // - Configure the brocker URL to be a local vm brocker
        .put("org.ops4j.pax.jms.property.BrokerURL", "vm://itest?create=true")
        // - Convert to Option
        .asOption(),
        // Add Configuration to kickstart Connectionprovider
        ConfigurationAdminOptions.newConfiguration("org.ops4j.pax.jms.JMSConnectionProvider").asOption(),
        //Open a debug port...
        CoreOptions.systemProperty("osgi.console").value("6080"));
    }

    @Before
    public void showMetaInfo() throws JMSException {
        ConnectionMetaData metaData = connection.getMetaData();
        LOG.debug("Provider:          {}", metaData.getJMSProviderName());
        LOG.debug("JMS Version:       {}", metaData.getJMSVersion());
        LOG.debug("Client ID:         {}", connection.getClientID());
        LOG.debug("ExceptionListener: {}", connection.getExceptionListener());
    }
}
