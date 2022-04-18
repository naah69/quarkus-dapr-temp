package org.jboss.resteasy.client.jaxrs.engines.dapr;

import io.quarkiverse.dapr.core.SyncDaprClient;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngineBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import javax.enterprise.inject.spi.CDI;

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
            return new DefaultDaprClientEngine(CDI.current().select(SyncDaprClient.class).get(), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
