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

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Test;

/**
 * The simple test just makes sure objects getting created/injected
 */
public class SimpleTest extends ITestBase {

    @Test
    public void createConnectionTest() throws JMSException {
        Connection connection = factory.createConnection();
        connection.close();
    }

    @Test
    public void createConnectionWithUserAndPasswordTest() throws JMSException {
        Connection connection = factory.createConnection("admin", "admin");
        connection.close();
    }

    @Test
    public void createSessionTest() throws JMSException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        session.close();
    }

    @Test(expected = JMSException.class)
    public void checkCantCloseSharedConnection() throws JMSException {
        connection.close();
    }

    @Test(expected = JMSException.class)
    public void checkCantsetClientID() throws JMSException {
        connection.setClientID("huja");
    }

    @Test(expected = JMSException.class)
    public void checkCantsetExceptionListener() throws JMSException {
        connection.setExceptionListener(new ExceptionListener() {

            @Override
            public void onException(JMSException ex) {

            }
        });
    }

    @Test(expected = JMSException.class)
    public void checkCantstart() throws JMSException {
        connection.start();
    }

    @Test(expected = JMSException.class)
    public void checkCantstop() throws JMSException {
        connection.stop();
    }
}
