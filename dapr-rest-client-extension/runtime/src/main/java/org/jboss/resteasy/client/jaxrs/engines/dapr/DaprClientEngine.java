package org.jboss.resteasy.client.jaxrs.engines.dapr;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;

/**
 * DaprClientEngine
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public interface DaprClientEngine extends ClientHttpEngine {
    /**
     * Enumeration to represent memory units.
     */
    enum MemoryUnit {
        /**
         * Bytes
         */
        BY,
        /**
         * Killo Bytes
         */
        KB,

        /**
         * Mega Bytes
         */
        MB,

        /**
         * Giga Bytes
         */
        GB
    }

    static DaprClientEngine create() {
        return new DefaultDaprClientEngine();
    }

}
