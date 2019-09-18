package com.axway.apim.rest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class swaggerpromote {
   @PostMapping(value = "/import", produces = { "application/json" })
   @ResponseBody
   public String importSwagger(@RequestParam(name = "stage") String stage,
         @RequestPart("configFile") MultipartFile configFile, 
         @RequestPart("apiDefinition") MultipartFile apiDefinition, 
         @RequestPart("hostname") String hostname) {

      String response = String.format("{\"stage\": \"%s\", \"hostname\": \"%s\", \"Files\": [{\"name\": \"%s\", \"Size\": \"%s\", \"Content-Type\": \"%s\"}, {\"name\": \"%s\", \"Size\": \"%s\", \"Content-Type\": \"%s\"}]}", 
         stage, hostname, configFile.getOriginalFilename(), configFile.getSize(), configFile.getContentType()
         , apiDefinition.getOriginalFilename(), apiDefinition.getSize(), apiDefinition.getContentType());
      return response;   
   }

   @GetMapping("/healthcheck")
   @ResponseBody
   public String healthcheck() {
      return "Hello Spring Boot";
   }
}
