/*******************************************************************************
 * Copyright 2011 Universidad Politécnica de Madrid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.universAAL.service.simplifiedAPIs.profileModel;

import java.util.HashMap;

import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.ServiceCall;
import org.universAAL.middleware.service.ServiceCallee;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.process.ProcessOutput;
import org.universAAL.middleware.service.owls.profile.ServiceProfile;

/**
 * This class will help manage service profiles easier.<br>
 * Each {@link ServiceProfile} will be implemented in a single class that
 * implements {@link ServiceProfileModel}.<br>
 * This service profile decoupling helps manage, from an implementation point of
 * view, all service profiles, and at the same time helps developers to register
 * and unregister dynamically service profiles.<br>
 * Also this {@link ServiceCallee} implements the typical service call checks,
 * so the amount of code copied is decreased, and therefore code becomes more
 * understandable and error proof.
 * 
 * @author amedrano
 * @see ServiceProfileModel
 */
public class DefaultServiceCallee extends ServiceCallee {

	/**
	 * The Map to keep track of the {@link ServiceProfileModel}s that are
	 * registered and their callbacks.
	 */
	private HashMap profileModels;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            {@link ModuleContext} mandatory for all {@link ServiceCallee},
	 * @param profileModel
	 *            a list of all initial {@link ServiceProfileModel}s that this
	 *            service callee manages.
	 */
	public DefaultServiceCallee(ModuleContext context,
			ServiceProfileModel[] profileModel) {
		super(context, new ServiceProfile[] {});
		profileModels = new HashMap();
		ServiceProfile[] realizedServices = new ServiceProfile[profileModel.length];
		for (int i = 0; i < profileModel.length; i++) {
			ServiceProfile sp = profileModel[i].getServiceProfile();
			profileModels.put(sp.getURI(), profileModel[i]);
			realizedServices[i] = sp;
		}
		addNewServiceProfiles(realizedServices);
	}

	/**
	 * add a new service profile, by providing a {@link ServiceProfileModel}.
	 * 
	 * @param spm
	 *            the {@link ServiceProfileModel} which implements the
	 *            {@link ServiceProfile} to be added.
	 */
	public final void addServiceProfileModel(ServiceProfileModel spm) {
		ServiceProfile sp = spm.getServiceProfile();
		profileModels.put(sp.getURI(), spm);
		addNewServiceProfiles(new ServiceProfile[] { sp });
	}

	/**
	 * Remove an existing service profile, by providing a
	 * {@link ServiceProfileModel}.
	 * 
	 * @param spm
	 *            the {@link ServiceProfileModel} which implements the
	 *            {@link ServiceProfile} to be removed.
	 */
	public final void removeServiceProfileModel(ServiceProfileModel spm) {
		ServiceProfile sp = spm.getServiceProfile();
		profileModels.remove(sp.getURI());
		removeMatchingProfiles(new ServiceProfile[] { sp });
	}

	/** {@inheritDoc} */
	public void communicationChannelBroken() {
		// nothing
	}

	/** {@inheritDoc} */
	public final ServiceResponse handleCall(ServiceCall call) {
		ServiceResponse invalidInput = new ServiceResponse(
				CallStatus.serviceSpecificFailure);
		if (call == null) {
			invalidInput
					.addOutput(new ProcessOutput(
							ServiceResponse.PROP_SERVICE_SPECIFIC_ERROR,
							"Corrupt call"));
			return invalidInput;
		}

		String operation = call.getProcessURI();
		/*
		 * operation may not be equal to profile URI! there might be necessary a
		 * transformation. In principle this works!
		 */
		if (operation == null) {
			invalidInput
					.addOutput(new ProcessOutput(
							ServiceResponse.PROP_SERVICE_SPECIFIC_ERROR,
							"Corrupt call"));
			return invalidInput;
		}

		if (!profileModels.containsKey(operation)) {
			invalidInput
					.addOutput(new ProcessOutput(
							ServiceResponse.PROP_SERVICE_SPECIFIC_ERROR,
							"Invalid call"));
			return invalidInput;
		}
		return ((ServiceProfileModel) profileModels.get(operation))
				.handleCall(call);
	}

}
