package com.axway.apim.rest.impl.api;

import java.io.InputStream;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;

import com.axway.apim.rest.api.ApiApiService;

@RequestScoped
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2019-09-17T17:42:06.365+02:00")
public class ApiApiServiceImpl implements ApiApiService {
      @Override
      public Response callImport(InputStream configFileInputStream, Attachment configFileDetail, InputStream apiDefinitionInputStream, Attachment apiDefinitionDetail, String stage, String hostname, Boolean force, String username, String password, SecurityContext securityContext) {
      // do some magic!
      return Response.ok().entity("magic!").build();
  }
      @Override
      public Response export(SecurityContext securityContext) {
      // do some magic!
      return Response.ok().entity("magic!").build();
  }
}
