package com.axway.apim.lib;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parameters {
	
	private static Logger LOG = LoggerFactory.getLogger(Parameters.class);
	
	public static String MODE_REPLACE	= "replace";
	public static String MODE_IGNORE	= "ignore";
	public static String MODE_ADD		= "add";
	
	private static Parameters instance;
	
	int port = 8075;
	
	private CommandLine cmd;
	
	private CommandLine internalCmd;
	
	private EnvironmentProperties envProperties;
	
	private Map<String, String> manualParams;
	
	/**
	 * Use this constructor manually build a CommandParameters instance. 
	 * This is useful when calling Swagger-Promote other classes or running tests.
	 * @param manualParams
	 */
	public Parameters (Map<String, String> manualParams) {
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
		if(getValue("username")!=null) {
			return (String)getValue("username");
		} else {
			// Perhaps the admin_username is given
			return (String)getValue("admin_username");
		}
	}

	public String getPassword() {
		if(getValue("password")!=null) {
			return (String)getValue("password");
		} else {
			// Perhaps the admin_password is given (hopefully in combination with the admin_username)
			return (String)getValue("admin_password");
		}
	}
	
	public String getAdminUsername() {
		return (String)getValue("admin_username");
	}

	public String getAdminPassword() {
		return (String)getValue("admin_password");
	}

	public String getHostname() {
		return (String)getValue("host");
	}

	public int getPort() {
		if(getValue("port")==null) return port;
		return Integer.parseInt((String)getValue("port"));
	}

	public boolean isEnforceBreakingChange() {
		if(getValue("force")==null) return false;
		return Boolean.parseBoolean((String)getValue("force"));
	}
	
	public boolean isIgnoreQuotas() {
		if(getValue("ignoreQuotas")==null) return false;
		return Boolean.parseBoolean((String)getValue("ignoreQuotas"));
	}
	
	public boolean isIgnoreClientApps() {
		if(getClientAppsMode().equals(MODE_IGNORE)) return true;
		return false;
	}
	
	public String getQuotaMode() {
		if(getValue("quotaMode")==null) return MODE_ADD;
		return ((String)getValue("quotaMode")).toLowerCase();
	}
	
	public String getClientAppsMode() {
		if(getValue("clientAppsMode")==null) return MODE_ADD;
		return ((String)getValue("clientAppsMode")).toLowerCase();
	}
	
	public boolean isIgnoreClientOrgs() {
		if(getClientOrgsMode().equals(MODE_IGNORE)) return true;
		return false;
	}
	
	public String getClientOrgsMode() {
		if(getValue("clientOrgsMode")==null) return MODE_ADD;
		return ((String)getValue("clientOrgsMode")).toLowerCase();
	}
	
	public String getAPIManagerURL() {
		return "https://"+this.getHostname()+":"+this.getPort();
	}
	
	public boolean ignoreAdminAccount() {
		if(getValue("ignoreAdminAccount")==null) return false;
		return Boolean.parseBoolean((String)getValue("ignoreAdminAccount"));
	}
	
	public String getDetailsExportFile() {
		if(getValue("detailsExportFile")==null) return null;
		return (String)getValue("detailsExportFile");
	}
	
	public boolean replaceHostInSwagger() {
		if(getValue("replaceHostInSwagger")==null) return true;
		return Boolean.parseBoolean((String)getValue("replaceHostInSwagger"));
	}
	
	public boolean rollback() {
		if(getValue("rollback")==null) return true;
		return Boolean.parseBoolean((String)getValue("rollback"));
	}
	
	public void validateRequiredParameters() throws AppException {
		ErrorState errors  = ErrorState.getInstance();
		if(getValue("username")==null && getValue("admin_username")==null) errors.setError("Required parameter: 'username' or 'admin_username' is missing.", ErrorCode.MISSING_PARAMETER, false);
		if(getValue("password")==null && getValue("admin_password")==null) errors.setError("Required parameter: 'password' or 'admin_password' is missing.", ErrorCode.MISSING_PARAMETER, false);
		if(getValue("host")==null) errors.setError("Required parameter: 'host' is missing.", ErrorCode.MISSING_PARAMETER, false);
		if(errors.hasError) {
			LOG.error("Provide at least the following parameters: username, password and host either using Command-Line-Options or in Environment.Properties");
			throw new AppException("Missing required parameters.", ErrorCode.MISSING_PARAMETER);
		}
	}
	
	public Object getValue(String key) {
		if(this.internalCmd!=null && this.cmd.getOptionValue(key)!=null) {
			return this.cmd.getOptionValue(key);
		} else if(this.internalCmd!=null && this.internalCmd.getOptionValue(key)!=null) {
			return this.internalCmd.getOptionValue(key);
		} else if(this.envProperties!=null && this.envProperties.containsKey(key)) {
			return this.envProperties.get(key);
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
