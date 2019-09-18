package com.axway.apim.rest.exception;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-09-18T10:12:08.028+02:00")

public class NotFoundException extends ApiException {
  private static final long serialVersionUID = 6386199328736022029L;
  private int code;

  public NotFoundException(int code, String msg) {
    super(code, msg);
    this.code = code;
  }
}
