package org.jboss.resteasy.client.jaxrs.engines.dapr;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngineBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import io.quarkiverse.dapr.core.SyncDaprClientManager;

/**
 * DaprClientHttpEngineBuilder
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class DaprClientHttpEngineBuilder implements ClientHttpEngineBuilder {

    private ResteasyClientBuilder that;

    @Override
    public ClientHttpEngineBuilder resteasyClientBuilder(ResteasyClientBuilder resteasyClientBuilder) {
        that = resteasyClientBuilder;
        return this;
    }

    @Override
    public ClientHttpEngine build() {

        try {
            // this may be null.  We can't really support this with Apache Client.
            return new DefaultDaprClientEngine(SyncDaprClientManager.getInstance(), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
