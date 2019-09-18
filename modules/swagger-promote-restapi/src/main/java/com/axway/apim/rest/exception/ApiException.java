package com.axway.apim.rest.exception;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-09-18T10:12:08.028+02:00")

public class ApiException extends Exception {
  private static final long serialVersionUID = -959671293641904402L;

  private int code;

  public ApiException(int code, String msg) {
    super(msg);
    this.code = code;
  }
}
