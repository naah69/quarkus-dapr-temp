package io.quarkiverse.dapr.rest.client.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * DaprRestClientExtensionProcessor
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
class DaprRestClientExtensionProcessor {

    private static final String FEATURE = "dapr-rest-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
