package org.jboss.resteasy.client.jaxrs.engines.dapr;

import org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;

import io.quarkiverse.dapr.core.SyncDaprClient;


/**
 * DaprClientHttpEngineBuilder
 *
 * An Dapr HTTP engine for use with the new Builder Config style.
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class DefaultDaprClientEngine extends ManualClosingDaprClientEngine {
    public DefaultDaprClientEngine() {
        super();
    }

    public DefaultDaprClientEngine(final SyncDaprClient httpClient) {
        super(httpClient);
    }

    public DefaultDaprClientEngine(final SyncDaprClient httpClient, final boolean closeHttpClient) {
        super(httpClient, closeHttpClient);
    }

    public DefaultDaprClientEngine(final SyncDaprClient httpClient, final HttpContextProvider httpContextProvider) {
        super(httpClient, httpContextProvider);
    }

    @Override
    public void finalize() throws Throwable {
        if (!closed && allowClosingHttpClient && daprClient != null)
            LogMessages.LOGGER.closingForYou(this.getClass());
        close();
        super.finalize();
    }

}
