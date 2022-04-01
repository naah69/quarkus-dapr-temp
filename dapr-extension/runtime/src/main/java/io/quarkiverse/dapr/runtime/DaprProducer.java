package io.quarkiverse.dapr.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.dapr.client.DaprClient;
import io.quarkiverse.dapr.core.DaprClientManager;
import io.quarkiverse.dapr.core.SyncDaprClient;
import io.quarkiverse.dapr.core.SyncDaprClientManager;

/**
 * DaprProducer
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
@ApplicationScoped
public class DaprProducer {

    @Produces
    @ApplicationScoped
    public DaprClient daprClient() {
        return DaprClientManager.getInstance();
    }

    @Produces
    @ApplicationScoped
    public SyncDaprClient syncDaprClient() {
        return SyncDaprClientManager.getInstance();
    }
}
