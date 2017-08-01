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
package org.ops4j.pax.jms.helper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueRequestor;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TopicRequestor;

/**
 * This is a more general form of the {@link TopicRequestor} and
 * {@link QueueRequestor} defined by the JMS spec. It completely works on the
 * generic {@link Destination} interface and use a {@link TemporaryTopic} or
 * {@link TemporaryQueue} for the response. Beside that, it provides a method to
 * request with a given timeout and uses generics to specify the expected
 * {@link Message} subtype.
 * 
 * @author Christoph Läubrich
 */
public class DestinationRequestor {

    private final boolean         useReplyQueue;
    private final Session         session;
    private final MessageProducer producer;

    /**
     * @throws JMSException
     */
    public DestinationRequestor(Session session, Destination destination) throws JMSException {
        this(session, destination, true);
    }

    public DestinationRequestor(Session session, Destination destination, boolean useReplyQueue) throws JMSException {
        this.session = session;
        this.useReplyQueue = useReplyQueue;
        producer = session.createProducer(destination);
    }

    /**
     * Closes the {@link DestinationRequestor} and its {@link Session}.
     */
    public void close() {
        try {
            producer.close();
        } catch (JMSException e) {
            //ignore it...
        }
        try {
            session.close();
        } catch (JMSException e) {
            //ignore it...
        }
    }

    /**
     * Sends a request and waits for a reply.
     * 
     * @throws JMSException
     */
    public Message request(Message message) throws JMSException {
        return request(message, Message.class);
    }

    /**
     * Sends a request and waits for a reply.
     * 
     * @throws JMSException
     * @throws TimeoutException
     */
    public Message request(Message message, TimeUnit unit, long value) throws JMSException, TimeoutException {
        return request(message, Message.class, unit, value);
    }

    /**
     * Sends a request and waits for a reply.
     * 
     * @throws JMSException
     */
    public <T extends Message> T request(Message message, Class<T> messageType) throws JMSException {
        try {
            return request(message, messageType, null, 0);
        } catch (TimeoutException e) {
            //This sould never happen for an infinite timeout!
            throw new JMSException("invalid state");
        }
    }

    /**
     * Sends a request and waits for a reply.
     * 
     * @throws JMSException
     */
    public <T extends Message> T request(Message message, Class<T> messageType, TimeUnit unit, long value) throws JMSException, TimeoutException {
        long maxWait = unit != null ? unit.toMillis(value) : 0;
        Destination replyDestination;
        if (useReplyQueue) {
            replyDestination = session.createTemporaryQueue();
        } else {
            replyDestination = session.createTemporaryTopic();
        }
        try {
            MessageConsumer consumer = session.createConsumer(replyDestination);
            try {
                message.setJMSReplyTo(replyDestination);
                synchronized (this) {
                    producer.setTimeToLive(maxWait);
                    producer.send(message);
                }
                Message receive = consumer.receive(maxWait);
                if (receive == null) {
                    //since there is no cuncurrent close of consumer, this can only be a timeout
                    throw new TimeoutException("receive message timed out");
                }
                try {
                    return messageType.cast(receive);
                } catch (ClassCastException e) {
                    throw new IllegalStateException("message from the channel was not the type " + message, e);
                }
            } finally {
                consumer.close();
            }
        } finally {
            try {
                if (useReplyQueue) {
                    ((TemporaryQueue) replyDestination).delete();
                } else {
                    ((TemporaryTopic) replyDestination).delete();
                }
            } catch (JMSException e) {
                //we can safely ignore this...
            }
        }
    }

}
