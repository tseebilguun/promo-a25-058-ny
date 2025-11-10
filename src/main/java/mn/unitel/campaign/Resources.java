package mn.unitel.campaign;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/data-addon")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Resources {
    @Inject
    Services services;

    @GET
    @Path("/getNewYearAddonList/{msisdn}")
    public Response getProgressByTokiId(@PathParam("msisdn") String msisdn) {
        return services.getNewYearAddonList(msisdn);
    }
}