package com.axway.apim.rest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.axway.apim.APIImportService;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.lib.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-09-18T14:19:58.449+02:00")

@Controller
public class ApiApiController implements ApiApi {

  private static final Logger log = LoggerFactory.getLogger(ApiApiController.class);

  private final ObjectMapper objectMapper;

  private final HttpServletRequest request;

  protected APIImportService apiImportService = new APIImportService();

  @org.springframework.beans.factory.annotation.Autowired
  public ApiApiController(ObjectMapper objectMapper, HttpServletRequest request) {
    this.objectMapper = objectMapper;
    this.request = request;
  }

  public ResponseEntity<Object> callImport(
      @ApiParam(value = "file detail") @Valid @RequestPart("configFile") MultipartFile configFile,
      @ApiParam(value = "file detail") @Valid @RequestPart("apiDefinition") MultipartFile apiDefinition,
      @ApiParam(value = "target Environment") @Valid @RequestParam(value = "stage", required = false) String stage,
      @ApiParam(value = "", defaultValue = "api-env.host.com") @Valid @RequestParam(value = "hostname", required = false, defaultValue = "api-env.host.com") String hostname,
      @ApiParam(value = "if true the desired API state take over the existing one in case of breaking state. If no, no changes made", defaultValue = "true") @Valid @RequestParam(value = "force", required = false, defaultValue = "true") Boolean force,
      @ApiParam(value = "login of API manager admin account") @RequestHeader(value = "username", required = false) String username,
      @ApiParam(value = "password of API manager admin account") @RequestHeader(value = "password", required = false) String password) {

    Map<String, Object> parameterMap = new HashMap<>();
    try {
      parameterMap.put("host", hostname);
      parameterMap.put("username", username);
      parameterMap.put("password", password);
      parameterMap.put("contract", configFile.getResource().getInputStream());
      parameterMap.put("apidefinition", apiDefinition.getResource().getInputStream());
    }
    catch (IOException e) {
      log.error("file error", e);
      return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    int apiImportServiceErrorCode = 0;
    synchronized (apiImportService) {
      // singleton objects
      new Parameters(parameterMap);
      try {
        apiImportServiceErrorCode = apiImportService.execute();
      }
      catch (AppException e) {
        log.error("apiImportService error", e);
        apiImportServiceErrorCode = e.getErrorCode().getCode();
      }
    }
    
    ResponseEntity<Object> response;
    if (apiImportServiceErrorCode > 0) {
      ErrorCode errorCode = ErrorCode.getInstance(apiImportServiceErrorCode);
      String body = String.format("{ \"error code\": \"%d\", \"error description\": \"%s\" }",
          errorCode.getCode(), errorCode.getDescription());
      response = new ResponseEntity<Object>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    else {
      response = new ResponseEntity<Object>(HttpStatus.OK);
    }
 
    return response;
  }

  public ResponseEntity<Object> export() {
    String accept = request.getHeader("Accept");
    if (accept != null && accept.contains("application/json")) {
      try {
        return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class), HttpStatus.NOT_IMPLEMENTED);
      } catch (IOException e) {
        log.error("Couldn't serialize response for content type application/json", e);
        return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    return new ResponseEntity<Object>(HttpStatus.OK);
  }

}
