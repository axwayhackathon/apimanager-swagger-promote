package com.axway.apim.lib;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parameters {
	
	private static Logger LOG = LoggerFactory.getLogger(Parameters.class);
	
	public enum ModeEnum {
	  replace,
	  ignore,
	  add
	}

	public enum ParameterEnum {
	  contract,
	  apiDefinition,
	  apiPath,
	  localFolder,
	  stage,
	  username,
	  adminUsername,
	  password,
	  adminPassword,
	  host,
	  port,
	  force,
	  ignoreQuotas,
	  quotaMode,
	  clientAppsMode,
	  clientOrgsMode,
	  ignoreAdminAccount,
	  detailsExportFile,
	  replaceHostInSwagger,
	  rollback
	}

	private static Parameters instance;
	
	int port = 8075;
	
	private CommandLine cmd;
	
	private CommandLine internalCmd;
	
	private EnvironmentProperties envProperties;
	
	private Map<ParameterEnum, Object> manualParams;
	
	/**
	 * Use this constructor manually build a CommandParameters instance. 
	 * This is useful when calling Swagger-Promote other classes or running tests.
	 * @param manualParams
	 */
	public Parameters (Map<ParameterEnum, Object> manualParams) {
		this.manualParams = manualParams;
		Parameters.instance = this;
	}
	
	public Parameters (CommandLine cmd) throws AppException {
		this(cmd, null, null);
	}
	
	public Parameters (CommandLine cmd, CommandLine internalCmd, EnvironmentProperties environment) throws AppException {
		this.cmd = cmd;
		this.internalCmd = internalCmd;
		this.envProperties = environment;
		validateRequiredParameters();
		Parameters.instance = this;
	}
	
	public static synchronized Parameters getInstance() {
		if(TestIndicator.getInstance().isTestRunning()) {
			return null; // Skip this, if executed as a test
		}
		if (Parameters.instance == null) {
			LOG.error("CommandParameters has not been initialized.");
			throw new RuntimeException("CommandParameters has not been initialized.");
		}
		return Parameters.instance;
	}

	public String getUsername() {
		if(getValue(ParameterEnum.username)!=null) {
			return (String)getValue(ParameterEnum.username);
		} else {
			// Perhaps the admin_username is given
			return (String)getValue(ParameterEnum.adminUsername);
		}
	}

	public String getPassword() {
		if(getValue(ParameterEnum.password)!=null) {
			return (String)getValue(ParameterEnum.password);
		} else {
			// Perhaps the admin_password is given (hopefully in combination with the admin_username)
			return (String)getValue(ParameterEnum.adminPassword);
		}
	}
	
	public String getAdminUsername() {
		return (String)getValue(ParameterEnum.adminUsername);
	}

	public String getAdminPassword() {
		return (String)getValue(ParameterEnum.adminPassword);
	}

	public String getHostname() {
		return (String)getValue(ParameterEnum.host);
	}

	public int getPort() {
		if(getValue(ParameterEnum.port)==null) return port;
		return Integer.parseInt((String)getValue(ParameterEnum.port));
	}

	public boolean isEnforceBreakingChange() {
		if(getValue(ParameterEnum.force)==null) return false;
		return Boolean.parseBoolean((String)getValue(ParameterEnum.force));
	}
	
	public boolean isIgnoreQuotas() {
		if(getValue(ParameterEnum.ignoreQuotas)==null) return false;
		return Boolean.parseBoolean((String)getValue(ParameterEnum.ignoreQuotas));
	}
	
	public boolean isIgnoreClientApps() {
		if(getClientAppsMode().equals(ModeEnum.ignore)) return true;
		return false;
	}
	
	public ModeEnum getQuotaMode() {
		if(getValue(ParameterEnum.quotaMode)==null) return ModeEnum.add;
    return ModeEnum.valueOf(getValue(ParameterEnum.quotaMode).toString());
	}
	
	public ModeEnum getClientAppsMode() {
		if(getValue(ParameterEnum.clientAppsMode)==null) return ModeEnum.add;
		return ModeEnum.valueOf(getValue(ParameterEnum.clientAppsMode).toString());
	}
	
	public boolean isIgnoreClientOrgs() {
		if(getClientOrgsMode().equals(ModeEnum.ignore)) return true;
		return false;
	}
	
	public ModeEnum getClientOrgsMode() {
		if(getValue(ParameterEnum.clientOrgsMode)==null) return ModeEnum.add;
    return ModeEnum.valueOf(getValue(ParameterEnum.clientOrgsMode).toString());
	}
	
	public String getAPIManagerURL() {
		return "https://"+this.getHostname()+":"+this.getPort();
	}
	
	public boolean ignoreAdminAccount() {
		if(getValue(ParameterEnum.ignoreAdminAccount)==null) return false;
		return Boolean.parseBoolean((String)getValue(ParameterEnum.ignoreAdminAccount));
	}
	
	public String getDetailsExportFile() {
		if(getValue(ParameterEnum.detailsExportFile)==null) return null;
		return (String)getValue(ParameterEnum.detailsExportFile);
	}
	
	public boolean replaceHostInSwagger() {
		if(getValue(ParameterEnum.replaceHostInSwagger)==null) return true;
		return Boolean.parseBoolean((String)getValue(ParameterEnum.replaceHostInSwagger));
	}
	
	public boolean rollback() {
		if(getValue(ParameterEnum.rollback)==null) return true;
		return Boolean.parseBoolean((String)getValue(ParameterEnum.rollback));
	}
	
	public void validateRequiredParameters() throws AppException {
		ErrorState errors  = ErrorState.getInstance();
		if(getValue(ParameterEnum.username)==null && getValue(ParameterEnum.adminUsername)==null) errors.setError("Required parameter: 'username' or 'admin_username' is missing.", ErrorCode.MISSING_PARAMETER, false);
		if(getValue(ParameterEnum.password)==null && getValue(ParameterEnum.adminPassword)==null) errors.setError("Required parameter: 'password' or 'admin_password' is missing.", ErrorCode.MISSING_PARAMETER, false);
		if(getValue(ParameterEnum.host)==null) errors.setError("Required parameter: 'host' is missing.", ErrorCode.MISSING_PARAMETER, false);
		if(errors.hasError) {
			LOG.error("Provide at least the following parameters: username, password and host either using Command-Line-Options or in Environment.Properties");
			throw new AppException("Missing required parameters.", ErrorCode.MISSING_PARAMETER);
		}
	}
	
	public Object getValue(ParameterEnum key) {
		if(this.internalCmd!=null && this.cmd.getOptionValue(key.name())!=null) {
			return this.cmd.getOptionValue(key.name());
		} else if(this.internalCmd!=null && this.internalCmd.getOptionValue(key.name())!=null) {
			return this.internalCmd.getOptionValue(key.name());
		} else if(this.envProperties!=null && this.envProperties.containsKey(key.name())) {
			return this.envProperties.get(key.name());
		} else if(this.manualParams!=null && this.manualParams.containsKey(key)) {
			return this.manualParams.get(key);
		} else {
			return null;
		}
	}
	
	public Map<String, String> getEnvironmentProperties() {
		return this.envProperties;
	}

	public void setEnvProperties(EnvironmentProperties envProperties) {
		this.envProperties = envProperties;
	}
}
