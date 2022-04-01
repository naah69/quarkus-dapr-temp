package io.quarkiverse.dapr.core;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;

/**
 * DaprClientManager
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class DaprClientManager {
    private static final DaprClient instance = new DaprClientBuilder().build();

    private DaprClientManager() {
    }

    public static final DaprClient getInstance() {
        return instance;
    }
}
