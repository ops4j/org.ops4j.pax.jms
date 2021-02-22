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
package org.ops4j.pax.jms.config.impl;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.jms.ConnectionFactory;

import org.jasypt.encryption.StringEncryptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.ops4j.pax.jms.config.ConfigLoader;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({
        "rawtypes", "unchecked"
})
public class ConnectionFactoryConfigManagerTest {

    private static final String ARTEMIS_CFF_FILTER = "(&(objectClass=org.ops4j.pax.jms.service.ConnectionFactoryFactory)(type=artemis))";
    private static final String TESTPID = "testpid";
    private BundleContext context;

    @Before
    public void setup() throws Exception {
        context = mock(BundleContext.class);
        when(context.createFilter(anyString()))
                .thenAnswer(invocation -> FrameworkUtil.createFilter(invocation.getArgument(0, String.class)));
        ServiceReference ref = mock(ServiceReference.class);
        ServiceReference[] refs = new ServiceReference[] { ref };
        String filter = "(" + Constants.OBJECTCLASS + "=" + ConfigLoader.class.getName() + ")";
        when(context.getServiceReferences((String) null, filter)).thenReturn(refs);
        when(context.getService(ref)).thenReturn(new FileConfigLoader());
    }

    @Test
    public void testUpdatedAndDeleted() throws Exception {
        ConnectionFactoryFactory cff = expectTracked(context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);
        ConnectionFactory cf = expectConnectionFactoryCreated(cff);
        ServiceRegistration sreg = expectRegistration(cf);

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");

        ConnectionFactoryConfigManager cfManager = new ConnectionFactoryConfigManager(context, new ExternalConfigLoader(context));

        cfManager.updated(TESTPID, properties);
        verify(context).addServiceListener(any(ServiceListener.class), eq(ARTEMIS_CFF_FILTER));

        cfManager.updated(TESTPID, null);

        verify(sreg).unregister();
        verify(context).removeServiceListener(any(ServiceListener.class));
        verify(context).ungetService(any(ServiceReference.class));
    }

    @Test
    public void testEncryptor() throws Exception {
        final ConnectionFactoryFactory cff = expectTracked(context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);
        ConnectionFactory cf = mock(ConnectionFactory.class);
        ArgumentCaptor<Map> capturedProps = ArgumentCaptor.forClass(Map.class);
        when(cff.createConnectionFactory(capturedProps.capture())).thenReturn(cf);
        expectRegistration(cf);

        StringEncryptor encryptor = expectTracked(context, StringEncryptor.class, "(objectClass=org.jasypt.encryption.StringEncryptor)");
        when(encryptor.decrypt("ciphertext")).thenReturn("password");

        ConnectionFactoryConfigManager cfManager = new ConnectionFactoryConfigManager(context, new ExternalConfigLoader(context));

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        properties.put(ConnectionFactoryFactory.JMS_PASSWORD, "ENC(ciphertext)");
        cfManager.updated(TESTPID, properties);
        verify(context).addServiceListener(any(ServiceListener.class), eq(ARTEMIS_CFF_FILTER));

        // the encrypted value is still encrypted
        assertEquals("ENC(ciphertext)", properties.get(ConnectionFactoryFactory.JMS_PASSWORD));

        assertEquals("password", capturedProps.getValue().get(ConnectionFactoryFactory.JMS_PASSWORD));
    }

    @Test
    public void testEncryptorWithExternalSecret() throws Exception {
        final ConnectionFactoryFactory cff = expectTracked(context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);
        ConnectionFactory cf = expectConnectionFactoryCreated(cff);
        expectRegistration(cf);
        StringEncryptor encryptor = expectTracked(context, StringEncryptor.class, "(objectClass=org.jasypt.encryption.StringEncryptor)");
        when(encryptor.decrypt("ciphertext")).thenReturn("password");

        ConnectionFactoryConfigManager cfManager = new ConnectionFactoryConfigManager(context, new ExternalConfigLoader(context));

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "artemis");
        String externalEncryptedValue = "FILE(" + ExternalConfigLoaderTest
                .createExternalSecret("ENC(ciphertext)") + ")";
        properties.put(ConnectionFactoryFactory.JMS_PASSWORD, externalEncryptedValue);
        cfManager.updated(TESTPID, properties);
        verify(context).addServiceListener(any(ServiceListener.class), eq(ARTEMIS_CFF_FILTER));

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
        final ConnectionFactoryFactory cff = expectTracked(context, ConnectionFactoryFactory.class, ARTEMIS_CFF_FILTER);

        final String keyHiddenJmsPassword = "." + ConnectionFactoryFactory.JMS_PASSWORD;
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
        properties.put(keyHiddenJmsPassword, password);
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

        ConnectionFactory cf = expectConnectionFactoryCreated(cff);

        Hashtable<String, String> expectedServiceProperties = (Hashtable<String, String>) properties.clone();
        expectedServiceProperties.remove(keyHiddenJmsPassword);
        expectedServiceProperties.put("osgi.jndi.service.name", valueConnectionFactoryName);
        ServiceRegistration sreg = mock(ServiceRegistration.class);
        when(context.registerService(anyString(), eq(cf), eq(expectedServiceProperties))).thenReturn(sreg);

        ConnectionFactoryConfigManager cfManager = new ConnectionFactoryConfigManager(context, new ExternalConfigLoader(context));

        cfManager.updated(TESTPID, properties);
        verify(context).addServiceListener(any(ServiceListener.class), eq(ARTEMIS_CFF_FILTER));
    }

    private <T> T expectTracked(BundleContext context, Class<T> iface, String expectedFilter) throws InvalidSyntaxException {
        final T serviceMock = mock(iface);
        ServiceReference ref = mock(ServiceReference.class);
        ServiceReference[] refs = new ServiceReference[] { ref };
        when(context.getServiceReferences((String) null, expectedFilter)).thenReturn(refs);
        when(context.getService(ref)).thenReturn(serviceMock);
        return serviceMock;
    }

    private ConnectionFactory expectConnectionFactoryCreated(final ConnectionFactoryFactory cff) throws SQLException {
        ConnectionFactory cf = mock(ConnectionFactory.class);
        when(cff.createConnectionFactory(any(Map.class))).thenReturn(cf);
        return cf;
    }

    private ServiceRegistration expectRegistration(ConnectionFactory cf) {
        ServiceRegistration sreg = mock(ServiceRegistration.class);
        when(context.registerService(anyString(), eq(cf), any(Dictionary.class))).thenReturn(sreg);
        return sreg;
    }

}
