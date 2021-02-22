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

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ConnectionFactoryRegistrationTest {

    private static final String H2_DRIVER_CLASS = "org.h2.Driver";

//    @Test
//    public void testPublishedAndUnpublished() throws ConfigurationException,
//        InvalidSyntaxException, SQLException {
//        Capture<Map<String, Object>> capturedCfProps = EasyMock.newCapture();
//        Capture<Dictionary> capturedServiceProps = EasyMock.newCapture();
//
//        IMocksControl c = EasyMock.createControl();
//        BundleContext context = c.createMock(BundleContext.class);
//        final ConnectionFactoryFactory cff = c.createMock(ConnectionFactoryFactory.class);
//
//        // Expect that a ConnectionFactory is created using the ConnectionFactoryFactory
//        ConnectionFactory cf = c.createMock(ConnectionFactory.class);
//        expect(cff.createConnectionFactory(capture(capturedCfProps))).andReturn(cf);
//
//        // Expect ConnectionFactory is registered as a service
//        ServiceRegistration cfSreg = c.createMock(ServiceRegistration.class);
//        expect(
//            context.registerService(eq(ConnectionFactory.class.getName()), eq(cf),
//                capture(capturedServiceProps))).andReturn(cfSreg);
//
//        // create and publish the ConnectionFactory
//        c.replay();
//        Dictionary<String, Object> properties = new Hashtable();
//        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
//        ConnectionFactoryRegistration publisher = new ConnectionFactoryRegistration(context, cff, properties, properties);
//        c.verify();
//
//        // Check that correct properties were set on the ConnectionFactory service
//        Dictionary serviceProps = capturedServiceProps.getValue();
//        assertEquals("mycfname", serviceProps.get(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME));
//        assertEquals("mycfname", serviceProps.get("osgi.jndi.service.name"));
//
//        c.reset();
//
//        // Check unpublish unregisters the services
//        cfSreg.unregister();
//        expectLastCall();
//
//        c.replay();
//        publisher.close();
//        c.verify();
//    }
//
//    @SuppressWarnings("resource")
//    @Test
//    public void testPublishedXACF() throws ConfigurationException, InvalidSyntaxException,
//        SQLException {
//
//        IMocksControl c = EasyMock.createControl();
//        BundleContext context = c.createMock(BundleContext.class);
//        final ConnectionFactoryFactory cff = c.createMock(ConnectionFactoryFactory.class);
//
//        // Expect that a ConnectionPoolConnectionFactory is created using the ConnectionFactoryFactory
//        XAConnectionFactory xacf = c.createMock(XAConnectionFactory.class);
//        expect(cff.createXAConnectionFactory(anyObject(Map.class))).andReturn(xacf);
//
//        // Expect ConnectionFactory is registered as a service
//        ServiceRegistration cfSreg = c.createMock(ServiceRegistration.class);
//        expect(
//            context.registerService(eq(XAConnectionFactory.class.getName()), eq(xacf),
//                anyObject(Dictionary.class))).andReturn(cfSreg);
//
//        // create and publish the ConnectionFactory
//        c.replay();
//        Dictionary<String, Object> properties = new Hashtable();
//        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
//        properties.put(ConnectionFactoryRegistration.CONNECTION_FACTORY_TYPE, XAConnectionFactory.class.getSimpleName());
//        new ConnectionFactoryRegistration(context, cff, properties, properties);
//        c.verify();
//    }
//
//    @SuppressWarnings("resource")
//    @Test(expected = IllegalArgumentException.class)
//    public void testError() throws ConfigurationException, InvalidSyntaxException, SQLException {
//
//        IMocksControl c = EasyMock.createControl();
//        BundleContext context = c.createMock(BundleContext.class);
//        final ConnectionFactoryFactory cff = c.createMock(ConnectionFactoryFactory.class);
//
//        // create and publish the ConnectionFactory
//        c.replay();
//        Dictionary<String, Object> properties = new Hashtable<>();
//        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
//        properties.put(ConnectionFactoryRegistration.CONNECTION_FACTORY_TYPE, "something else");
//        new ConnectionFactoryRegistration(context, cff, properties, properties);
//        c.verify();
//    }

}
