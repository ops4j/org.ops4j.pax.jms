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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ConnectionFactoryRegistrationTest {

    private static final String H2_DRIVER_CLASS = "org.h2.Driver";

    @Test
    public void testPublishedAndUnpublished() throws ConfigurationException,
        InvalidSyntaxException, SQLException {
        Capture<Map<String, Object>> capturedDsProps = EasyMock.newCapture();
        Capture<Dictionary> capturedServiceProps = EasyMock.newCapture();

        IMocksControl c = EasyMock.createControl();
        BundleContext context = c.createMock(BundleContext.class);
        final ConnectionFactoryFactory dsf = c.createMock(ConnectionFactoryFactory.class);

        // Expect that a ConnectionFactory is created using the ConnectionFactoryFactory
        ConnectionFactory ds = c.createMock(ConnectionFactory.class);
        expect(dsf.createConnectionFactory(capture(capturedDsProps))).andReturn(ds);

        // Expect ConnectionFactory is registered as a service
        ServiceRegistration dsSreg = c.createMock(ServiceRegistration.class);
        expect(
            context.registerService(eq(ConnectionFactory.class.getName()), eq(ds),
                capture(capturedServiceProps))).andReturn(dsSreg);

        // create and publish the ConnectionFactory
        c.replay();
        Dictionary<String, Object> properties = new Hashtable();
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, "mycfname");
        ConnectionFactoryRegistration publisher = new ConnectionFactoryRegistration(context, dsf, properties, properties);
        c.verify();

        // Check that correct properties were set on the ConnectionFactory service
        Dictionary serviceProps = capturedServiceProps.getValue();
        assertEquals("mycfname", serviceProps.get(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME));
        assertEquals("mycfname", serviceProps.get("osgi.jndi.service.name"));

        c.reset();

        // Check unpublish unregisters the services
        dsSreg.unregister();
        expectLastCall();

        c.replay();
        publisher.close();
        c.verify();
    }

    @SuppressWarnings("resource")
    @Test
    public void testPublishedXADS() throws ConfigurationException, InvalidSyntaxException,
        SQLException {

        IMocksControl c = EasyMock.createControl();
        BundleContext context = c.createMock(BundleContext.class);
        final ConnectionFactoryFactory dsf = c.createMock(ConnectionFactoryFactory.class);

        // Expect that a ConnectionPoolConnectionFactory is created using the ConnectionFactoryFactory
        XAConnectionFactory xads = c.createMock(XAConnectionFactory.class);
        expect(dsf.createXAConnectionFactory(anyObject(Map.class))).andReturn(xads);

        // Expect ConnectionFactory is registered as a service
        ServiceRegistration dsSreg = c.createMock(ServiceRegistration.class);
        expect(
            context.registerService(eq(XAConnectionFactory.class.getName()), eq(xads),
                anyObject(Dictionary.class))).andReturn(dsSreg);

        // create and publish the ConnectionFactory
        c.replay();
        Dictionary<String, Object> properties = new Hashtable();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryRegistration.CONNECTION_FACTORY_TYPE, XAConnectionFactory.class.getSimpleName());
        new ConnectionFactoryRegistration(context, dsf, properties, properties);
        c.verify();
    }

    @SuppressWarnings("resource")
    @Test(expected = IllegalArgumentException.class)
    public void testError() throws ConfigurationException, InvalidSyntaxException, SQLException {

        IMocksControl c = EasyMock.createControl();
        BundleContext context = c.createMock(BundleContext.class);
        final ConnectionFactoryFactory dsf = c.createMock(ConnectionFactoryFactory.class);

        // create and publish the ConnectionFactory
        c.replay();
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(ConnectionFactoryRegistration.JNDI_SERVICE_NAME, "test");
        properties.put(ConnectionFactoryRegistration.CONNECTION_FACTORY_TYPE, "something else");
        new ConnectionFactoryRegistration(context, dsf, properties, properties);
        c.verify();
    }

}
