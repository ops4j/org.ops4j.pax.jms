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
package org.ops4j.pax.jms.example;

import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Läubrich
 */
public class ExampleMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleMessageSender.class);

    private Connection          connection;

    private Thread              sendThread;

    public void start() {
        sendThread = Executors.defaultThreadFactory().newThread(new Runnable() {

            @Override
            public void run() {
                try {
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    try {
                        MessageProducer producer = session.createProducer(null);
                        TextMessage textMessage = session.createTextMessage();
                        textMessage.setText("Hello simple example");
                        producer.send(session.createQueue("simple.example.queue"), textMessage);
                    } finally {
                        session.close();
                    }
                } catch (JMSException e) {
                    LOG.error("Problem while executing task: {}", e.getMessage(), e);
                }
            }
        });
        sendThread.start();

    }

    public void stop() {
        sendThread.interrupt();
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
