/*
 * #%L
 * OPS4J Pax JMS core bundle
 * %%
 * Copyright (C) 2013 OPS4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
package org.ops4j.pax.jms.core;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Läubrich
 */
public class MessageListenerHandler {

    private static final String                                PROPERTY_JMS_SESSION_POOLSIZE = "org.ops4j.pax.jms.poolsize";

    private static final int                                   DEFAULT_MAXIMUM_POOL_SIZE     = 10;

    private static final Logger                                LOG                           = LoggerFactory.getLogger(MessageListenerHandler.class);

    private final AtomicReference<Connection>                  connection                    = new AtomicReference<Connection>();

    private final Map<MessageListener, MessageListenerContext> contextMap                    = new IdentityHashMap<MessageListener, MessageListenerContext>();

    private final ThreadPoolExecutor                           sessionExecutionThread        = new ThreadPoolExecutor(0, DEFAULT_MAXIMUM_POOL_SIZE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

    protected void start(Map<String, ?> settings) {
        configure(settings);
    }

    /**
     * @param settings
     */
    protected void configure(Map<String, ?> settings) {
        int maxPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
        Object maxPoolSizeObject = settings.get(PROPERTY_JMS_SESSION_POOLSIZE);
        if (maxPoolSizeObject != null) {
            try {
                maxPoolSize = Integer.parseInt(maxPoolSizeObject.toString());
            } catch (NumberFormatException e) {
                LOG.warn("Can't parse {} property, using default", PROPERTY_JMS_SESSION_POOLSIZE, e);
            }
        }
        sessionExecutionThread.setMaximumPoolSize(maxPoolSize);

    }

    protected void shutdown() {
        //If the component is shut down, there might be still MessageListenerContext active (because of the dynamic modifier)
        //We just wait here untill all of them are gone, and the shut down the executor
        Executors.defaultThreadFactory().newThread(new Runnable() {

            @Override
            public void run() {
                LOG.debug("Gracefull shutdown of MessageListenerHandler started...");
                while (true) {
                    synchronized (contextMap) {
                        if (contextMap.isEmpty()) {
                            break;
                        }
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                sessionExecutionThread.shutdown();
                LOG.debug("Gracefull shutdown of MessageListenerHandler done.");
            }
        });
    }

    protected void setConnection(Connection connection) {
        this.connection.set(connection);
    }

    protected void addMessageListener(MessageListener messageListener, Map<String, ?> settings) {
        synchronized (contextMap) {
            final MessageListenerContext context = new MessageListenerContext(messageListener, settings);
            contextMap.put(messageListener, context);
            sessionExecutionThread.execute(new Runnable() {

                @Override
                public void run() {
                    Connection con;
                    while ((con = connection.get()) == null) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    try {
                        context.startContext(con);
                    } catch (JMSException e) {
                        LOG.error("JMS problem while starting MessageListenerContext: {}", e.getMessage(), e);
                        context.stopContext();
                    }
                }
            });
        }
    }

    protected void removeMessageListener(MessageListener messageListener) {
        synchronized (contextMap) {
            final MessageListenerContext context = contextMap.remove(messageListener);
            sessionExecutionThread.execute(new Runnable() {

                @Override
                public void run() {
                    context.stopContext();
                }
            });
        }
    }

    private static class MessageListenerContext {

        private static final String   PROPERTY_JMS_QUEUE              = "org.ops4j.pax.jms.consumer.queue";

        private static final String   PROPERTY_JMS_TOPIC              = "org.ops4j.pax.jms.consumer.topic";

        private static final String   PROPERTY_JMS_MESSAGE_SELECTOR   = "org.ops4j.pax.jms.consumer.selector";

        private static final String   PROPERTY_JMS_MESSAGE_NONLOCAL   = "org.ops4j.pax.jms.consumer.nonlocal";

        private static final String   PROPERTY_JMS_SESSION_MODE       = "org.ops4j.pax.jms.session.mode";

        public static final String    PROPERTY_JMS_SESSION_TRANSACTED = "org.ops4j.pax.jms.session.transacted";

        private final MessageListener messageListener;
        private boolean               stopped;
        private Session               session;

        private final boolean         transacted;
        private final int             acknowledgeMode;

        private final String          messageSelector;
        private final boolean         noLocal;
        private final String          topic;
        private final String          queue;

        /**
         * @param messageListener
         * @param settings
         */
        public MessageListenerContext(MessageListener messageListener, Map<String, ?> settings) {
            this.messageListener = messageListener;
            topic = (String) settings.get(PROPERTY_JMS_TOPIC);
            queue = (String) settings.get(PROPERTY_JMS_QUEUE);
            if (topic == null && queue == null) {
                throw new IllegalArgumentException("either " + PROPERTY_JMS_TOPIC + " or " + PROPERTY_JMS_QUEUE + " must be given as service property");
            }
            if (topic != null && queue != null) {
                throw new IllegalArgumentException("onyl one of " + PROPERTY_JMS_TOPIC + " or " + PROPERTY_JMS_QUEUE + " can be given as service property");
            }
            Object modeObject = settings.get(PROPERTY_JMS_SESSION_MODE);
            if (modeObject != null) {
                String modeString = modeObject.toString().trim();
                if (modeString.equals("AUTO_ACKNOWLEDGE") || modeString.equals(String.valueOf(Session.AUTO_ACKNOWLEDGE))) {
                    acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
                } else if (modeString.equals("CLIENT_ACKNOWLEDGE") || modeString.equals(String.valueOf(Session.CLIENT_ACKNOWLEDGE))) {
                    acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
                } else if (modeString.equals("DUPS_OK_ACKNOWLEDGE") || modeString.equals(String.valueOf(Session.DUPS_OK_ACKNOWLEDGE))) {
                    acknowledgeMode = Session.DUPS_OK_ACKNOWLEDGE;
                } else {
                    throw new IllegalArgumentException("Unsupported value for " + PROPERTY_JMS_SESSION_MODE + " (value = '" + modeString + "')");
                }
            } else {
                //TODO: Should we allow to configure the default?
                acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
            }
            //TODO: Should we allow to configure the default via MessageListenerHandler?
            transacted = Boolean.parseBoolean(String.valueOf(settings.get(PROPERTY_JMS_SESSION_TRANSACTED)));
            noLocal = Boolean.parseBoolean(String.valueOf(settings.get(PROPERTY_JMS_MESSAGE_NONLOCAL)));
            messageSelector = (String) settings.get(PROPERTY_JMS_MESSAGE_SELECTOR);
        }

        private synchronized void startContext(Connection connection) throws JMSException {
            if (stopped) {
                return;
            }
            session = connection.createSession(transacted, acknowledgeMode);
            Destination destination;
            if (topic != null) {
                destination = session.createTopic(topic.trim());
            } else {
                destination = session.createQueue(queue.trim());
            }
            MessageConsumer consumer = session.createConsumer(destination, messageSelector, noLocal);
            consumer.setMessageListener(messageListener);
            LOG.info("MessageListener {}@{} is now active on destination {} (transacted = {}, acknowledgeMode = {}, messageSelector = {}, NoLocal = {}).", new Object[] {
                    messageListener.getClass().getName(), System.identityHashCode(messageListener), destination, transacted, acknowledgeMode, messageSelector,
                    noLocal });
        }

        private synchronized void stopContext() {
            stopped = true;
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                }
                LOG.info("MessageListener {}@{} is now shut down.", messageListener.getClass().getName(), System.identityHashCode(messageListener));
                session = null;
            }
        }

    }
}
