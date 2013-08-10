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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Läubrich
 */
public class SimpleExampleListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleExampleListener.class);

    @Override
    public void onMessage(Message message) {
        try {
            LOG.info("A message arrived at my channel {}", message.getJMSDestination());
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                LOG.info("The TextMessage contains {}", text);
            }
        } catch (JMSException e) {
            LOG.error("error in onMessage :-(", e);
        }
    }

}
