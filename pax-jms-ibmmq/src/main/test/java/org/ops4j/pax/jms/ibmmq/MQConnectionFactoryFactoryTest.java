package org.ops4j.pax.jms.ibmmq;

import org.junit.Test;

import java.util.Hashtable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MQConnectionFactoryFactoryTest {

    @Test
    public void testConnectionNameList() {
        final Map<String, Object> props = new Hashtable<>();
        props.put("connectionNameList", "localhost:1414,172.0.0.1,172.0.0.2");
        MQConnectionFactoryFactory.configureConnectionNameList(props);
        assertEquals("localhost(1414),172.0.0.1,172.0.0.2", props.get("connectionNameList"));
    }

    @Test
    public void testNoConnectionNameList() {
        final Map<String, Object> props = new Hashtable<>();
        props.put("hostName", "localhost");
        props.put("port", "1414");
        MQConnectionFactoryFactory.configureConnectionNameList(props);
        assertNull(props.get("connectionNameList"));
    }

    @Test
    public void testSingleHostConnectionNameList() {
        final Map<String, Object> props = new Hashtable<>();
        props.put("connectionNameList", "localhost:1414");
        MQConnectionFactoryFactory.configureConnectionNameList(props);
        assertEquals("localhost(1414)", props.get("connectionNameList"));
    }

    @Test
    public void testSingleHostOnlyConnectionNameList() {
        final Map<String, Object> props = new Hashtable<>();
        props.put("connectionNameList", "localhost");
        MQConnectionFactoryFactory.configureConnectionNameList(props);
        assertEquals("localhost", props.get("connectionNameList"));
    }
}
