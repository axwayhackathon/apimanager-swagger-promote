package com.axway.apim.rest.api;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2019-09-17T17:42:06.365+02:00")
public interface ApiApiService {
      public Response callImport(InputStream configFileInputStream, Attachment configFileDetail, InputStream apiDefinitionInputStream, Attachment apiDefinitionDetail, String stage, String hostname, Boolean force, String username, String password, SecurityContext securityContext);
      public Response export(SecurityContext securityContext);
}
