/**
 * NOTE: This class is auto generated by the swagger code generator program (2.4.0-SNAPSHOT).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package com.axway.apim.rest.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-09-18T14:19:58.449+02:00")

@Api(value = "api", description = "the api API")
public interface ApiApi {

  @ApiOperation(value = "Replicate the desired API state", nickname = "callImport", notes = "", response = Object.class, tags = {})
  @ApiResponses(value = { @ApiResponse(code = 200, message = "", response = Object.class) })
  @RequestMapping(value = "api", produces = { "application/json" }, consumes = {
      "multipart/form-data" }, method = RequestMethod.POST)
  ResponseEntity<Object> callImport(
      @ApiParam(value = "file detail") @Valid @RequestPart("file") MultipartFile configFile,
      @ApiParam(value = "file detail") @Valid @RequestPart("file") MultipartFile apiDefinition,
      @ApiParam(value = "target Environment") @Valid @RequestParam(value = "stage", required = false) String stage,
      @ApiParam(value = "", defaultValue = "api-env.host.com") @Valid @RequestParam(value = "hostname", required = false, defaultValue = "api-env.host.com") String hostname,
      @ApiParam(value = "if true the desired API state take over the existing one in case of breaking state. If no, no changes made", defaultValue = "true") @Valid @RequestParam(value = "force", required = false, defaultValue = "true") Boolean force,
      @ApiParam(value = "login of API manager admin account") @RequestHeader(value = "username", required = false) String username,
      @ApiParam(value = "password of API manager admin account") @RequestHeader(value = "password", required = false) String password);

  @ApiOperation(value = "Get the actual API", nickname = "export", notes = "", response = Object.class, tags = {})
  @ApiResponses(value = { @ApiResponse(code = 200, message = "", response = Object.class) })
  @RequestMapping(value = "api", method = RequestMethod.GET)
  ResponseEntity<Object> export();

}