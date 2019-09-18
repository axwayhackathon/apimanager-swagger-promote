package com.axway.apim.rest.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;

/**
 * Home redirection to swagger api documentation
 */
@RestController
@Api(value = "api")
public class HomeController {
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String index() {
    return "service is working";
  }
}
