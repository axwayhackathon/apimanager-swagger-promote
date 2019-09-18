package com.axway.apim.swagger.config;

import java.io.File;
import java.io.InputStream;

import com.axway.apim.lib.AppException;

public class ConfigHandlerFactory {

	public static ConfigHandlerInterface getConfigHandler (Object apiConfig, Object apiDefinition, String stage, boolean orgAdminUsed) throws AppException {
		if(apiConfig instanceof String && 
				(new File((String)apiConfig).exists()) || ((String)apiConfig).startsWith("..")) {
			return new FileConfigHandler((String)apiConfig, (String)apiDefinition, stage, orgAdminUsed);
		} else {
			return new StreamConfigHandler((InputStream)apiConfig, (InputStream)apiDefinition, stage, orgAdminUsed);
		}
	}
}
