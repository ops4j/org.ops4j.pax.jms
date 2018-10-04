package org.ops4j.pax.jms.oracleaq;

import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;

public class Activator implements BundleActivator {

    ServiceRegistration<ConnectionFactoryFactory> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, "oracleaq");
        registration = context.registerService(ConnectionFactoryFactory.class,
                new OracleAQConnectionFactoryFactory(),
                props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
    }

}
