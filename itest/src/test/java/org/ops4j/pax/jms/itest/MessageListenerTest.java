/**
 * 
 */
package org.ops4j.pax.jms.itest;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Test the messagelistener facility of Pax JMS
 */
//TODO for some reason maven can't compile tests with generics in OSGi Services
@SuppressWarnings("rawtypes")
public class MessageListenerTest extends ITestBase {

    /**
     * Maximum number of millisecondt to wait for the service beeing fetched
     */
    private static final long                   MAX_WAIT_FETCHED = 60L * 1000L;

    /**
     * Maximum number of millisecondt to wait for the message to arive
     */
    private static final long                   MAX_WAIT_MESSAGE = 60L * 1000L;

    /**
     * Testmessage to use
     */
    private static final String                 ITEST_MESSAGE    = "This is an Itest Message";

    private ITestMesssageListenerServiceFactory messageListenerFactory;

    private ServiceRegistration                 registerService;
    private ITestMesssageListener               messsageListener;

    /**
     * Check that the service in fact is fetched by the core bundle
     * 
     * @throws InterruptedException
     */
    @Test(timeout = MAX_WAIT_FETCHED)
    public void checkServiceIsFetched() throws InterruptedException {
        while (!messageListenerFactory.bundlesFetchedService.contains("org.ops4j.pax.jms.core")) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
    }

    @Test(timeout = MAX_WAIT_MESSAGE)
    public void checkSendMessage() throws JMSException, InterruptedException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        try {
            Queue queue = session.createQueue(MessageListenerTest.class.getName().toLowerCase());
            MessageProducer producer = session.createProducer(queue);
            TextMessage textMessage = session.createTextMessage();
            textMessage.setText(ITEST_MESSAGE);
            producer.send(textMessage);
            while (!messsageListener.messages.contains(ITEST_MESSAGE)) {
                TimeUnit.MILLISECONDS.sleep(200);
            }
        } finally {
            session.close();
        }
    }

    @Before
    public void register() {
        messsageListener = new ITestMesssageListener();
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("org.ops4j.pax.jms.consumer.queue", MessageListenerTest.class.getName().toLowerCase());
        messageListenerFactory = new ITestMesssageListenerServiceFactory(messsageListener);
        registerService = bundleContext.registerService(MessageListener.class.getName(), messageListenerFactory, properties);
    }

    @After
    public void unregister() {
        registerService.unregister();
    }

    public static class ITestMesssageListenerServiceFactory implements ServiceFactory {

        private final Set<String>           bundlesFetchedService = new CopyOnWriteArraySet<String>();
        private final ITestMesssageListener service;

        public ITestMesssageListenerServiceFactory(ITestMesssageListener messsageListener) {
            service = messsageListener;
        }

        @Override
        public MessageListener getService(Bundle bundle, ServiceRegistration registration) {
            bundlesFetchedService.add(bundle.getSymbolicName());
            return service;
        }

        @Override
        public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            bundlesFetchedService.remove(bundle.getSymbolicName());
        }

    }

    public static class ITestMesssageListener implements MessageListener {

        private final Set<String> messages = new CopyOnWriteArraySet<String>();

        @Override
        public void onMessage(Message message) {
            if (message instanceof TextMessage) {
                try {
                    messages.add(((TextMessage) message).getText());
                } catch (JMSException e) {
                    // ignore here...
                }
            }
        }

    }

}
