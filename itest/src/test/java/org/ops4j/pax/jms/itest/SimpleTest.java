/**
 * 
 */
package org.ops4j.pax.jms.itest;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Test;

/**
 * @author Christoph Läubrich
 */
public class SimpleTest extends ITestBase {

    @Test
    public void createConnectionTest() throws JMSException {
        Connection connection = factory.createConnection();
        connection.close();
    }

    @Test
    public void createSessionTest() throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        session.close();
    }

}
