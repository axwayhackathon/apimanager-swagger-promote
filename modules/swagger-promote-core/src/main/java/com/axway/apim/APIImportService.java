package com.axway.apim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.actions.rest.APIMHttpClient;
import com.axway.apim.actions.rest.Transaction;
import com.axway.apim.lib.APIPropertiesExport;
import com.axway.apim.lib.AppException;
import com.axway.apim.lib.Parameters;
import com.axway.apim.lib.ErrorState;
import com.axway.apim.lib.rollback.RollbackHandler;
import com.axway.apim.swagger.APIChangeState;
import com.axway.apim.swagger.APIImportConfigAdapter;
import com.axway.apim.swagger.APIManagerAdapter;
import com.axway.apim.swagger.api.state.IAPI;
import com.axway.apim.swagger.config.ConfigHandlerFactory;
import com.axway.apim.swagger.config.ConfigHandlerInterface;

/**
 * Main class used to replicate the desired API into the API-Manager
 * @author cwiechmann
 *
 */
public class APIImportService {
	
	private static Logger LOG = LoggerFactory.getLogger(APIImportService.class);
	
	public int execute() throws AppException {
		// Reset Singletons as we need to reset the state of some variables such as AllAPIs, ALLOrgs, etc.
		APIManagerAdapter.deleteInstance();
		ErrorState.deleteInstance();
		APIMHttpClient.deleteInstance();
		Transaction.deleteInstance();
		RollbackHandler.deleteInstance();
		
		APIManagerAdapter apimAdapter = APIManagerAdapter.getInstance();

		Parameters params = Parameters.getInstance();
		ConfigHandlerInterface configHandler = ConfigHandlerFactory.getConfigHandler(params.getValue("contract"), params.getValue("apidefinition"), (String)params.getValue("stage"), apimAdapter.isUsingOrgAdmin());
		APIImportConfigAdapter configAdapter = new APIImportConfigAdapter(configHandler.getConfig());
		// Creates an API-Representation of the desired API
		IAPI desiredAPI = configAdapter.getDesiredAPI();
		// Lookup an existing APIs - If found the actualAPI is valid - desiredAPI is used to control what needs to be loaded
		IAPI actualAPI = apimAdapter.getAPIManagerAPI(apimAdapter.getExistingAPI(desiredAPI.getPath(), null, APIManagerAdapter.TYPE_FRONT_END), desiredAPI);
		// Based on the actual API - fulfill/complete some elements in the desired API
		configAdapter.completeDesiredAPI(desiredAPI, actualAPI);
		APIChangeState changeActions = new APIChangeState(actualAPI, desiredAPI);
		apimAdapter.applyChanges(changeActions);
		APIPropertiesExport.getInstance().store();
		LOG.info("Successfully replicated API-State into API-Manager");
		return 0;
	}
}
