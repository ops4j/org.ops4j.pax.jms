package org.ops4j.pax.jms.oracleaq;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.JMSContext;
import javax.jms.XAJMSContext;

import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.service.internal.BeanConfig;

public class OracleAQConnectionFactoryFactory implements ConnectionFactoryFactory {

    private static final String ORACLE_AQ_FACTORY_CLASS = "oracle.jms.AQjmsFactory";
    private final Class<?> oracleAqConnectionFactoryClass;
    
    public OracleAQConnectionFactoryFactory() throws ClassNotFoundException {
        ClassLoader classLoader = OracleAQConnectionFactoryFactory.class.getClassLoader();
        this.oracleAqConnectionFactoryClass = classLoader.loadClass(ORACLE_AQ_FACTORY_CLASS);
    }
    
    @Override
    public ConnectionFactory createConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {

    	String url = (String) props.remove(ConnectionFactoryFactory.JMS_URL);
        String user = (String) props.remove(ConnectionFactoryFactory.JMS_USER);
        String password = (String) props.remove(ConnectionFactoryFactory.JMS_PASSWORD);
        
        if (url == null || user==null || password==null) {
            throw new JMSRuntimeException("The url, user and password properties must be set");
        }
        
        Properties properties = new Properties();
        properties.setProperty(ConnectionFactoryFactory.JMS_USER,user);
        properties.setProperty(ConnectionFactoryFactory.JMS_PASSWORD,password);
      
		try {
			ConnectionFactory cf= ConnectionFactory.class.cast(oracleAqConnectionFactoryClass.getDeclaredMethod("getConnectionFactory",String.class,Properties.class).invoke(null,url, properties));
			return cf;
        } catch (Exception e) {
        	throw (JMSRuntimeException) new JMSRuntimeException("Unable to build Oracle AQ ConnectionFactory").initCause(e);
        }
    }

    @Override
    public XAConnectionFactory createXAConnectionFactory(Map<String, Object> props) throws JMSRuntimeException {
       	
    	String url = (String) props.remove(ConnectionFactoryFactory.JMS_URL);
        String user = (String) props.remove(ConnectionFactoryFactory.JMS_USER);
        String password = (String) props.remove(ConnectionFactoryFactory.JMS_PASSWORD);
        
        if (url == null || user==null || password==null) {
            throw new JMSRuntimeException("The url, user and password properties must be set");
        }
        
        Properties properties = new Properties();
        properties.setProperty(ConnectionFactoryFactory.JMS_USER,user);
        properties.setProperty(ConnectionFactoryFactory.JMS_PASSWORD,password);
        
		try {
			XAConnectionFactory xaCf= XAConnectionFactory.class.cast(oracleAqConnectionFactoryClass.getDeclaredMethod("getXAConnectionFactory",String.class,Properties.class).invoke(null,url, properties));
			return xaCf;
        } catch (Exception e) {
        	throw (JMSRuntimeException) new JMSRuntimeException("Unable to build Oracle AQ ConnectionFactory").initCause(e);
        }
    }
    
}
