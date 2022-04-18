package io.quarkus.dapr.restclient.runtime;

import io.quarkus.restclient.runtime.QuarkusRestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

/**
 * Created by hbraun on 22.01.18.
 */
public class BuilderResolver extends RestClientBuilderResolver {
    @Override
    public RestClientBuilder newBuilder() {
        return new DaprRestClientBuilder();
    }
}
