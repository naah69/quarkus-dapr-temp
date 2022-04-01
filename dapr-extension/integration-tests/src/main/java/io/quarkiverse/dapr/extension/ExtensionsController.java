package io.quarkiverse.dapr.extension;

public class ExtensionsController implements ExtensionsService {
    @Override
    public String getById(String id) {
        return id;
    }

    @Override
    public Extension put(Extension extension) {
        extension.name = "dapr-test";
        return extension;
    }
}
