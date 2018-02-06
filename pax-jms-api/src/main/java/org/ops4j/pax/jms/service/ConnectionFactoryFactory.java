/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.ops4j.pax.jms.service;

import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnectionFactory;
import java.util.Map;

/**
 * A factory for JMS ConnectionFactory
 */
public interface ConnectionFactoryFactory {

    String JMS_CONNECTIONFACTORY_NAME   = "name";

    String JMS_CONNECTIONFACTORY_TYPE   = "type";

    /**
     * Specify which protocol to use. For example when using Artemis, the AMQP protocol may be used
     * by setting the 'amqp' value.
     */
    String JMS_CONNECTIONFACTORY_PROTOCOL   = "protocol";

    /**
     * The "user" property that ConnectionFactory clients should supply a value for
     * when calling {@link #createConnectionFactory(Map)}.
     */
    String	JMS_USER					= "user";

    /**
     * The "password" property that ConnectionFactory clients should supply a value for
     * when calling {@link #createConnectionFactory(Map)}.
     */
    String	JMS_PASSWORD				= "password";

    /**
     * The "url" property that ConnectionFactory clients should supply a value for when
     * calling {@link #createConnectionFactory(Map)}.
     */
    String	JMS_URL					    = "url";

    /**
     * The "initialPoolSize" property that ConnectionFactory and
     * XAConnectionFactory clients may supply a value for when calling
     * {@link #createConnectionFactory(Map)} or
     * {@link #createXAConnectionFactory(Map)} on drivers that support this
     * property.
     */
    String	JMS_INITIAL_POOL_SIZE		= "initialPoolSize";

    /**
     * The "maxIdleTime" property that ConnectionFactory and XAConnectionFactory
     * clients may supply a value for when calling
     * {@link #createConnectionFactory(Map)} or
     * {@link #createXAConnectionFactory(Map)} on drivers that support this
     * property.
     */
    String	JMS_MAX_IDLE_TIME			= "maxIdleTime";

    /**
     * The "maxPoolSize" property that ConnectionFactory and XAConnectionFactory
     * clients may supply a value for when calling
     * {@link #createConnectionFactory(Map)} or
     * {@link #createXAConnectionFactory(Map)} on drivers that support this
     * property.
     */
    String	JMS_MAX_POOL_SIZE			= "maxPoolSize";

    /**
     * The "maxStatements" property that ConnectionFactory and XAConnectionFactory
     * clients may supply a value for when calling
     * {@link #createConnectionFactory(Map)} or
     * {@link #createXAConnectionFactory(Map)} on drivers that support this
     * property.
     */
    String	JMS_MAX_STATEMENTS			= "maxStatements";

    /**
     * The "minPoolSize" property that ConnectionFactory and XAConnectionFactory
     * clients may supply a value for when calling
     * {@link #createConnectionFactory(Map)} or
     * {@link #createXAConnectionFactory(Map)} on drivers that support this
     * property.
     */
    String	JMS_MIN_POOL_SIZE			= "minPoolSize";

    /**
     * Create a new {@code ConnectionFactory} using the given properties.
     *
     * @param props The properties used to configure the {@code ConnectionFactory} .
     *        {@code null} indicates no properties. If the property cannot be
     *        set on the {@code ConnectionFactory} being created then a
     *        {@code JMSRuntimeException} must be thrown.
     * @return A configured {@code ConnectionFactory}.
     * @throws JMSRuntimeException If the {@code ConnectionFactory} cannot be created.
     */
    ConnectionFactory createConnectionFactory(Map<String, Object> props) throws JMSRuntimeException;

    /**
     * Create a new {@code ConnectionPoolDataSource} using the given properties.
     *
     * @param props The properties used to configure the
     *        {@code XAConnectionFactory}. {@code null} indicates no
     *        properties. If the property cannot be set on the
     *        {@code XAConnectionFactory} being created then a
     *        {@code JMSRuntimeException} must be thrown.
     * @return A configured {@code ConnectionPoolDataSource}.
     * @throws JMSRuntimeException If the {@code XAConnectionFactory} cannot be
     *         created.
     */
    XAConnectionFactory createXAConnectionFactory(Map<String, Object> props) throws JMSRuntimeException;

}
