/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ops4j.pax.jms.service;

import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.jms.XAConnectionFactory;

/**
 * A factory for pooled JMS {@link ConnectionFactory}. It's an equivalent of pax-jdbc's
 * {@code org.ops4j.pax.jdbc.pool.common.PooledDataSourceFactory}
 */
public interface PooledConnectionFactoryFactory {

    /**
     * A logical name (key) of registered {@code PooledConnectionFactoryFactory}
     */
    String POOL_KEY = "pool";

    /**
     * A boolean flag indicating whether the registered {@code PooledConnectionFactoryFactory} is or is not XA-Aware.
     */
    String XA_KEY = "xa";

    /**
     * Method similar to {@link ConnectionFactoryFactory} factory methods.
     * It creates pooled {@link ConnectionFactory} using {@link ConnectionFactoryFactory}.
     * @param cff existing {@link ConnectionFactoryFactory} that can be used to create {@link ConnectionFactory} or
     * {@link XAConnectionFactory} depending on configuration properties
     * @param props pooling and connection factory configuration
     * @return poolable {@link ConnectionFactory}
     * @throws JMSRuntimeException
     */
    ConnectionFactory create(ConnectionFactoryFactory cff, Map<String, Object> props) throws JMSRuntimeException;

}
