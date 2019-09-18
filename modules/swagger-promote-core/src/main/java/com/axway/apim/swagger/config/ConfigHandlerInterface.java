package com.axway.apim.swagger.config;

import com.axway.apim.lib.AppException;

public interface ConfigHandlerInterface {
	public APIConfig getConfig() throws AppException;
}
