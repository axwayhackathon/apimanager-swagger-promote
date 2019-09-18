package com.axway.apim.rest.controller;

import java.io.IOException;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-09-18T14:19:58.449+02:00")

@Controller
public class ApiApiController implements ApiApi {

  private static final Logger log = LoggerFactory.getLogger(ApiApiController.class);

  private final ObjectMapper objectMapper;

  private final HttpServletRequest request;

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
    String accept = request.getHeader("Accept");
    if (accept != null && accept.contains("application/json")) {
      try {
        return new ResponseEntity<Object>(objectMapper.readValue("\"{}\"", Object.class), HttpStatus.NOT_IMPLEMENTED);
      } catch (IOException e) {
        log.error("Couldn't serialize response for content type application/json", e);
        return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
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
