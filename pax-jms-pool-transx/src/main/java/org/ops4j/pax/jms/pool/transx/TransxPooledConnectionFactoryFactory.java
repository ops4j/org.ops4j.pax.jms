/*
 * Copyright 2021 OPS4J.
 *
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
 */
package org.ops4j.pax.jms.pool.transx;

import java.util.Map;
import javax.jms.ConnectionFactory;
import javax.jms.JMSRuntimeException;
import javax.resource.spi.TransactionSupport;

import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.ops4j.pax.jms.service.PooledConnectionFactoryFactory;
import org.ops4j.pax.transx.jms.ManagedConnectionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.jms.service.internal.BeanConfig.getNonPoolProps;
import static org.ops4j.pax.jms.service.internal.BeanConfig.getPoolProps;

public class TransxPooledConnectionFactoryFactory implements PooledConnectionFactoryFactory {

    public static final Logger LOG = LoggerFactory.getLogger(TransxPooledConnectionFactoryFactory.class);

    @Override
    public ConnectionFactory create(ConnectionFactoryFactory cff, Map<String, Object> props) throws JMSRuntimeException {
        try {
            ConnectionFactory cf = cff.createConnectionFactory(getNonPoolProps(props));
            ConnectionFactory mcf = ManagedConnectionFactoryBuilder.builder()
                    .connectionFactory(cf, null)
                    .transaction(TransactionSupport.TransactionSupportLevel.NoTransaction)
                    .properties(getPoolProps(props))
                    .build();
            // TODO: configure more
            return mcf;
        } catch (Throwable e) {
            LOG.error("Error creating pooled connection factory: " + e.getMessage(), e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

}
