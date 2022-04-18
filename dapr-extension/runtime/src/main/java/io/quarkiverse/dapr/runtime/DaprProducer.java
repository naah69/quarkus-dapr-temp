package io.quarkiverse.dapr.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.quarkiverse.dapr.core.SyncDaprClient;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.Startup;

/**
 * DaprProducer
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
@ApplicationScoped
public class DaprProducer {

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public DaprClient daprClient() {
        return new DaprClientBuilder().build();
    }

    @Produces
    @DefaultBean
    @Startup
    @Singleton
    @Unremovable
    public SyncDaprClient syncDaprClient(DaprClient client) {
        return new SyncDaprClient(client);
    }
}
