package org.addressbook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path(".well-known")
public class WellKnownResource {

    @Path("/carddav")
    @PROPFIND
    public Response welKnownService(String body) {
        Response.ResponseBuilder builder = Response.status(301).entity("Redirected to /carddav/");
        builder.header("Location", "/carddav/");
        return builder.build();
    }

}
