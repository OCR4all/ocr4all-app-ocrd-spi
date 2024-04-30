/**
 * File:     OCRDMsaServiceProviderModelWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     10.04.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa;

import java.util.Hashtable;
import java.util.List;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;

/**
 * Defines ocr-d microservice architecture (MSA) service provider workers with
 * model support. The following properties of the service provider collection
 * <b>ocr-d</b> override the local default settings (<b>key</b>: <i>default
 * value</i>):
 * <ul>
 * <li>see {@link OCRDServiceProviderWorker} for settings</li>
 * </ul>
 * 
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public abstract class OCRDMsaServiceProviderModelWorker extends OCRDMsaServiceProviderWorker {
	/**
	 * The model argument.
	 */
	private final String modelArgument;

	/**
	 * Creates an ocr-d microservice architecture (MSA) service provider workers
	 * with model support.
	 * 
	 * @param logger        The logger class.
	 * @param modelArgument The model argument.
	 * @since 1.8
	 */
	public OCRDMsaServiceProviderModelWorker(Class<?> logger, String modelArgument) {
		super(logger);

		this.modelArgument = modelArgument;
	}

	/**
	 * Returns the default model. Extending classes can overwrite this method to set
	 * a default model.
	 * 
	 * @return The default model. Null, if there is no default model.
	 * @since 1.8
	 */
	protected ConfigurationServiceProvider.CollectionKey getDefaultModel() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.
	 * OCRDMsaServiceProviderWorker#getPremise(de.uniwuerzburg.zpd.ocr4all.
	 * application.spi.env.Target)
	 */
	@Override
	public Premise getPremise(Target target) {
		return getOptResourcesFolders(configuration, target).isEmpty()
				? new Premise(Premise.State.warn,
						locale -> "There are no models available in the ocr-d opt directory '"
								+ getOptResources(configuration, target).toString() + "'.")
				: super.getPremise(target);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.
	 * OCRDMsaServiceProviderWorker#getModelCallbacks(de.uniwuerzburg.zpd.ocr4all.
	 * application.spi.env.Target, java.util.List)
	 */
	@Override
	protected Hashtable<String, ProviderDescription.ModelFactory.ModelFieldCallback> getModelCallbacks(Target target,
			List<String> arguments) {
		if (arguments.contains(modelArgument)) {
			// The models
			ProviderDescription.ModelFactory.ModelFieldCallback modelsCallback = getOptResourcesFolderFieldCallback(
					configuration, target, ConfigurationServiceProvider.getValue(configuration, getDefaultModel()));

			Hashtable<String, ProviderDescription.ModelFactory.ModelFieldCallback> callbacks = new Hashtable<>();
			callbacks.put(modelArgument, modelsCallback);

			return callbacks;
		} else
			return null;
	}

}
