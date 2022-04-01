package io.quarkiverse.dapr.core;

/**
 * SyncDaprClientManager
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class SyncDaprClientManager {
    private static final SyncDaprClient instance = new SyncDaprClient(DaprClientManager.getInstance());

    private SyncDaprClientManager() {
    }

    public static final SyncDaprClient getInstance() {
        return instance;
    }
}
