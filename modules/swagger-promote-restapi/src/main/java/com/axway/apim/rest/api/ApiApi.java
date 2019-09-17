package com.axway.apim.rest.api;

import java.io.InputStream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
@Path("/api")
@RequestScoped

@Api(description = "the api API")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2019-09-17T17:42:06.365+02:00")

public class ApiApi  {

  @Context SecurityContext securityContext;

  @Inject ApiApiService delegate;


    @POST
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Replicate the desired API state", notes = "", response = Object.class, tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "", response = Object.class) })
    public Response callImport( @Multipart(value = "configFile") InputStream configFileInputStream, @Multipart(value = "configFile" ) Attachment configFileDetail,  @Multipart(value = "apiDefinition", required = false) InputStream apiDefinitionInputStream, @Multipart(value = "apiDefinition" , required = false) Attachment apiDefinitionDetail,  @ApiParam(value = "")  @QueryParam("stage") String stage,  @ApiParam(value = "", defaultValue="api-env.host.com") @DefaultValue("api-env.host.com") @QueryParam("hostname") String hostname,  @ApiParam(value = "", defaultValue="true") @DefaultValue("true") @QueryParam("force") Boolean force, @ApiParam(value = "" )@HeaderParam("username") String username, @ApiParam(value = "" )@HeaderParam("password") String password) {
        return delegate.callImport(configFileInputStream, configFileDetail, apiDefinitionInputStream, apiDefinitionDetail, stage, hostname, force, username, password, securityContext);
    }

    @GET
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Get the actual API", notes = "", response = Object.class, tags={  })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "", response = Object.class) })
    public Response export() {
        return delegate.export(securityContext);
    }
}
