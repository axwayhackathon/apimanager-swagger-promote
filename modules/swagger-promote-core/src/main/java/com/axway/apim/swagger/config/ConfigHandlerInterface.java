package com.axway.apim.swagger.config;

import java.io.InputStream;
import java.security.cert.X509Certificate;

import com.axway.apim.lib.AppException;
import com.axway.apim.swagger.api.properties.authenticationProfiles.AuthenticationProfile;
import com.axway.apim.swagger.api.properties.cacerts.CaCert;
import com.axway.apim.swagger.api.state.DesiredAPI;
import com.axway.apim.swagger.api.state.IAPI;

public interface ConfigHandlerInterface {
	public DesiredAPI getApiConfig() throws AppException;
	public boolean isOrgAdminUsed();
	public InputStream getInputStreamForCert(CaCert cert) throws AppException;
	public X509Certificate getX509Certificate(AuthenticationProfile authnProfile) throws AppException;
	public IAPI addImageContent(IAPI importApi) throws AppException;
}
