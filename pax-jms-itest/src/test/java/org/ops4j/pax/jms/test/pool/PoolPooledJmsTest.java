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
package org.ops4j.pax.jms.test.pool;

import javax.inject.Inject;

import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.jms.service.PooledConnectionFactoryFactory;
import org.ops4j.pax.jms.test.AbstractJmsTest;

import static org.ops4j.pax.exam.OptionUtils.combine;

public class PoolPooledJmsTest extends AbstractJmsTest {

    @Inject
    @Filter("(pool=pooledjms)(xa=false)")
    PooledConnectionFactoryFactory pool;

    @Configuration
    public Option[] config() {
        return combine(
                baseConfiguration(),
                CoreOptions.bootDelegationPackage("sun.*"),
                mvnBundle("org.ops4j.pax.jms", "pax-jms-api"),
                mvnBundle("org.ops4j.pax.jms", "pax-jms-pool-pooledjms"),
                mvnBundle("javax.transaction", "javax.transaction-api"),
                mvnBundle("javax.el", "javax.el-api"),
                mvnBundle("javax.interceptor", "javax.interceptor-api"),
                mvnBundle("javax.enterprise", "cdi-api"),
                mvnBundle("javax.jms", "javax.jms-api"),
                mvnBundle("org.messaginghub", "pooled-jms"),
                mvnBundle("org.apache.commons", "commons-pool2")
        );
    }

    @Test
    public void testPooledConnectionFactoryFactoryServicePresent() {
    }

}
