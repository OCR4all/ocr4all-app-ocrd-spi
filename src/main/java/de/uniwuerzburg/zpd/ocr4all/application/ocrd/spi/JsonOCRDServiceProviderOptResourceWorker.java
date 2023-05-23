/**
 * File:     JsonOCRDServiceProviderOptResourceWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     09.05.2023
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi;

import java.util.Hashtable;
import java.util.List;

import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;

/**
 * Defines ocr-d service provider workers with JSON and opt resource support.
 * The following properties of the service provider collection <b>ocr-d</b>
 * override the local default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>json: -J</li>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * <li>docker-stop-wait-kill-seconds: 2</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public abstract class JsonOCRDServiceProviderOptResourceWorker extends JsonOCRDServiceProviderWorker {
	/**
	 * The model argument.
	 */
	private final String modelArgument;

	/**
	 * Creates an ocr-d service provider worker with JSON and opt resource support.
	 * 
	 * @param name          The service provider name.
	 * @param modelArgument The model argument.
	 * @since 1.8
	 */
	public JsonOCRDServiceProviderOptResourceWorker(String name, String modelArgument) {
		super(name, true);

		this.modelArgument = modelArgument;
	}

	/**
	 * Returns the default model. Extending classes can overwrite this method to set
	 * a default model.
	 * 
	 * @return The default model.
	 * @since 1.8
	 */
	protected String getDefaultModel() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getPremise(
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target)
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
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.
	 * JsonOCRDServiceProviderWorker#getModelCallbacks(de.uniwuerzburg.zpd.ocr4all.
	 * application.spi.env.Target, java.util.List)
	 */
	@Override
	protected Hashtable<String, ModelFieldCallback> getModelCallbacks(Target target, List<String> arguments) {
		if (arguments.contains(modelArgument)) {
			// The models
			ModelFieldCallback modelsCallback = getOptResourcesFolderFieldCallback(configuration, target,
					getDefaultModel());

			Hashtable<String, ModelFieldCallback> callbacks = new Hashtable<>();
			callbacks.put(modelArgument, modelsCallback);

			return callbacks;
		} else
			return null;
	}

}
