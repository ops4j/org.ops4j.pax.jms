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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
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

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * This is the base for all itest, setting up the Pax JMS core for test
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ITestBase {

    @Rule
    public TestName testName = new TestName();

    protected static final Logger LOG = LoggerFactory.getLogger(ITestBase.class);

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

    @Before
    public void beforeEach() {
        LOG.info("========== Running {}.{}() ==========", getClass().getName(), testName.getMethodName());
    }

    @After
    public void afterEach() {
        LOG.info("========== Finished {}.{}() ==========\n", getClass().getName(), testName.getMethodName());
    }

    @Configuration
    public Option[] configure() {
        return CoreOptions.options(
                // basic options
                bootDelegationPackage("sun.*"),
                bootDelegationPackage("com.sun.*"),
                frameworkStartLevel(START_LEVEL_TEST_BUNDLE),
                workingDirectory("target/paxexam"),
                cleanCaches(true),
                systemTimeout(60 * 60 * 1000),

                systemProperty("org.ops4j.pax.logging.property.file").value("src/test/resources/pax-logging.properties"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
                frameworkProperty("felix.bootdelegation.implicit").value("false"),
                // set to "4" to see Felix wiring information
                frameworkProperty("felix.log.level").value("1"),

                // added implicitly by pax-exam, if pax.exam.system=test
                // these resources are provided inside org.ops4j.pax.exam:pax-exam-link-mvn jar
                url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                // configadmin should start before org.ops4j.pax.logging.pax-logging-log4j2
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject().startLevel(1).start(),

                // added implicitly by pax-exam, if pax.exam.system=test
                //  - url("link:classpath:META-INF/links/org.ops4j.pax.logging.api.link").startLevel( START_LEVEL_SYSTEM_BUNDLES),
                //  - url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link") .startLevel(START_LEVEL_SYSTEM_BUNDLES),
                //  - url("link:classpath:META-INF/links/org.osgi.compendium.link").startLevel( START_LEVEL_SYSTEM_BUNDLES),
                // but we will use versions aligned to pax-web:
                linkBundle("org.ops4j.pax.logging.pax-logging-api").startLevel(1).start(),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-log4j2").versionAsInProject().startLevel(2).start(),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javax-inject").startLevel(1).start(),

                CoreOptions.junitBundles(),

                //JMS spec
                mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jms_2.0_spec").versionAsInProject(),
                // We require DS
                mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject().startLevel(1).start(true),
                // and we want CM...
                //Active MQ as JMS provider, wrapped as bundle
                mavenBundle("javax.annotation", "javax.annotation-api").versionAsInProject(),
                mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-all").versionAsInProject(),
                mavenBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.api").versionAsInProject(),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core").versionAsInProject(),

                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-core").versionAsInProject(),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-beans").versionAsInProject(),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.spring-context").versionAsInProject(),
                mavenBundle("org.apache.activemq", "activemq-osgi").versionAsInProject(),

                // JSE 8
                CoreOptions.frameworkProperty("org.osgi.framework.system.capabilities").value("osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0,1.1,1.2,1.3,1.4,1.5,1.6,1.7,1.8,9.0\""),
                //Pax JMS core
                mavenBundle("org.ops4j.pax.jms", "org.ops4j.pax.jms.core").versionAsInProject(),
                // Add Configuration to setup ActiveMQ
                ConfigurationAdminOptions.newConfiguration("org.ops4j.pax.jms.ConfigurationBasedConnectionFactoryProvider")
                    // - Configure factory to use
                    .put("org.ops4j.pax.jms.factoryclass", "org.apache.activemq.ActiveMQConnectionFactory")
                    // - Configure the brocker URL to be a local vm brocker
                    .put("org.ops4j.pax.jms.property.BrokerURL", "vm://itest")
                    // - Also boolean properties can be set ...
                    .put("org.ops4j.pax.jms.property.CopyMessageOnSend", true)
                    //- as well as ints
                    .put("org.ops4j.pax.jms.property.SendTimeout", 0)
                    //- as well as longs
                    .put("org.ops4j.pax.jms.property.WarnAboutUnstartedConnectionTimeout", 1000L)
                    // - Convert to Option
                    .asOption(),
                // Add Configuration to kickstart Connectionprovider
                ConfigurationAdminOptions.newConfiguration("org.ops4j.pax.jms.JMSConnectionProvider")
                        .asOption(),
                // Open a debug port...
                CoreOptions.systemProperty("osgi.console").value("6080")
        );
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
