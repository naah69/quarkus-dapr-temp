package io.quarkiverse.dapr.extension;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    String getById(@QueryParam String id);

    @POST
    Extension put(Extension extension);
}
