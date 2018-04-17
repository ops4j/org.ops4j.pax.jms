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
package org.ops4j.pax.jms.config.impl;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.jasypt.encryption.StringEncryptor;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({
        "rawtypes", "unchecked"
})
public class ConnectionFactoryConfigManagerTest {

    private static final String ARTEMIS_CFF_FILTER = "(&(objectClass=org.ops4j.pax.jms.service.ConnectionFactoryFactory)(type=artemis))";
    private static final String TESTPID = "testpid";
    private IMocksControl c;
    private BundleContext context;

    @Before
    public void setup() throws Exception {
        c = EasyMock.createControl();
        context = c.createMock(BundleContext.class);
        Capture<String> capture = newCapture();
        expect(context.createFilter(EasyMock.capture(capture)))
                .andStubAnswer(() -> FrameworkUtil.createFilter(capture.getValue()));
    }

    @Test
    public void testUpdatedAndDeleted() throws Exception {
        ConnectionFactoryFactory dsf = expectTracked(c, context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);
        ConnectionFactory ds = expectConnectionFactoryCreated(dsf);
        ServiceRegistration sreg = expectRegistration(ds);

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");

        ConnectionFactoryConfigManager dsManager = new ConnectionFactoryConfigManager(context);

        // Test config created
        c.replay();
        dsManager.updated(TESTPID, properties);

        c.verify();

        c.reset();

        context.removeServiceListener(anyObject(ServiceListener.class));
        expectLastCall().atLeastOnce();
        sreg.unregister();
        expectLastCall();
        expect(context.ungetService(anyObject(ServiceReference.class))).andReturn(true).atLeastOnce();
        // Test config removed
        c.replay();
        dsManager.updated(TESTPID, null);

        c.verify();
    }

    @Test
    public void testEncryptor() throws Exception {
        final ConnectionFactoryFactory dsf = expectTracked(c, context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);
        ConnectionFactory ds = c.createMock(ConnectionFactory.class);
        Capture<Map<String, Object>> capturedProps = newCapture();
        expect(dsf.createConnectionFactory(EasyMock.capture(capturedProps))).andReturn(ds);
        expectRegistration(ds);

        StringEncryptor encryptor = expectTracked(c, context, StringEncryptor.class, "(objectClass=org.jasypt.encryption.StringEncryptor)");
        expect(encryptor.decrypt("ciphertext")).andReturn("password");

        ConnectionFactoryConfigManager dsManager = new ConnectionFactoryConfigManager(context);

        // Test config created
        c.replay();
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        properties.put(ConnectionFactoryFactory.JMS_PASSWORD, "ENC(ciphertext)");
        dsManager.updated(TESTPID, properties);
        c.verify();

        // the encrypted value is still encrypted
        assertEquals("ENC(ciphertext)", properties.get(ConnectionFactoryFactory.JMS_PASSWORD));

        assertEquals("password", capturedProps.getValue().get(ConnectionFactoryFactory.JMS_PASSWORD));
    }

    @Test
    public void testEncryptorWithExternalSecret() throws Exception {
        final ConnectionFactoryFactory dsf = expectTracked(c, context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);
        ConnectionFactory ds = expectConnectionFactoryCreated(dsf);
        expectRegistration(ds);
        ConnectionFactoryConfigManager dsManager = new ConnectionFactoryConfigManager(context);
        StringEncryptor encryptor = expectTracked(c, context, StringEncryptor.class, "(objectClass=org.jasypt.encryption.StringEncryptor)");
        expect(encryptor.decrypt("ciphertext")).andReturn("password");

        // Test config created
        c.replay();

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        String externalEncryptedValue = "FILE(" + ExternalConfigLoaderTest
                .createExternalSecret("ENC(ciphertext)") + ")";
        properties.put(ConnectionFactoryFactory.JMS_PASSWORD, externalEncryptedValue);
        dsManager.updated(TESTPID, properties);
        c.verify();

        // the encrypted/external value is still encrypted/external
        assertEquals(externalEncryptedValue, properties.get(ConnectionFactoryFactory.JMS_PASSWORD));
    }

    /**
     * Tests: - hidden properties (starting with a dot) are not added to service registry. - nonlocal
     * properties (containing a dot) are not propagated to
     * {@link ConnectionFactoryFactory#createConnectionFactory(Map)}. - local properties (not containing a dot) are
     * propagated to {@link ConnectionFactoryFactory#createConnectionFactory(Map)}.
     *
     * @throws Exception
     */
    @Test
    public void testHiddenAndPropagation() throws Exception {
        final ConnectionFactoryFactory dsf = expectTracked(c, context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);

        final String keyHiddenJdbcPassword = "." + ConnectionFactoryFactory.JMS_PASSWORD;
        final String keyNonlocalProperty = "nonlocal.property";
        final String keyLocalProperty = "localproperty";
        final String keyConnectionFactoryType = "ConnectionFactoryType";
        final String keyPoolProperty = "pool.maxTotal";
        final String keyFactoryProperty = "factory.poolStatements";
        final String valueLocalProperty = "something2";
        final String cfname = "mycfname";
        final String password = "thepassword";
        final String user = "theuser";
        final String poolMaxTotal = "10";
        final String factoryPoolStatements = "true";

        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, cfname);
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        properties.put(ConnectionFactoryFactory.JMS_USER, user);
        properties.put(keyHiddenJdbcPassword, password);
        properties.put(keyLocalProperty, valueLocalProperty);
        properties.put(keyNonlocalProperty, "something");
        properties.put(keyPoolProperty, poolMaxTotal);
        properties.put(keyFactoryProperty, factoryPoolStatements);

        // Exceptions local properties not being forwarded
        final String valueConnectionFactoryName = "myConnectionFactory";
        properties.put(keyConnectionFactoryType, "ConnectionFactory");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, valueConnectionFactoryName);
        properties.put(ConnectionFactoryRegistration.MANAGED_CF, "true");

        Properties expectedConnectionFactoryProperties = new Properties();
        expectedConnectionFactoryProperties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, cfname);
        expectedConnectionFactoryProperties.put(ConnectionFactoryFactory.JMS_USER, user);
        expectedConnectionFactoryProperties.put(ConnectionFactoryFactory.JMS_PASSWORD, password);
        expectedConnectionFactoryProperties.put(keyLocalProperty, valueLocalProperty);
        expectedConnectionFactoryProperties.put(keyPoolProperty, poolMaxTotal);
        expectedConnectionFactoryProperties.put(keyFactoryProperty, factoryPoolStatements);

        ConnectionFactory ds = expectConnectionFactoryCreated(dsf);

        Hashtable<String, String> expectedServiceProperties = (Hashtable<String, String>) properties.clone();
        expectedServiceProperties.remove(keyHiddenJdbcPassword);
        expectedServiceProperties.put("osgi.jndi.service.name", valueConnectionFactoryName);
        ServiceRegistration sreg = c.createMock(ServiceRegistration.class);
        expect(context.registerService(anyString(), eq(ds), eq(expectedServiceProperties))).andReturn(sreg);

        ConnectionFactoryConfigManager dsManager = new ConnectionFactoryConfigManager(context);

        // Test config created
        c.replay();
        dsManager.updated(TESTPID, properties);
        c.verify();
    }

    private <T> T expectTracked(IMocksControl c, BundleContext context, Class<T> iface, String expectedFilter)
            throws InvalidSyntaxException {
        final T serviceMock = c.createMock(iface);
        ServiceReference ref = c.createMock(ServiceReference.class);
        context.addServiceListener(anyObject(ServiceListener.class), eq(expectedFilter));
        expectLastCall();
        ServiceReference[] refs = new ServiceReference[] {
                ref
        };
        expect(context.getServiceReferences((String) null, expectedFilter)).andReturn(refs);
        expect(context.getService(ref)).andReturn(serviceMock);
        return serviceMock;
    }

    private ConnectionFactory expectConnectionFactoryCreated(final ConnectionFactoryFactory dsf) throws SQLException {
        ConnectionFactory ds = c.createMock(ConnectionFactory.class);
        expect(dsf.createConnectionFactory(anyObject(Map.class))).andReturn(ds);
        return ds;
    }

    private ServiceRegistration expectRegistration(ConnectionFactory ds) {
        ServiceRegistration sreg = c.createMock(ServiceRegistration.class);
        expect(context.registerService(anyString(), eq(ds), anyObject(Dictionary.class))).andReturn(sreg);
        return sreg;
    }

}
