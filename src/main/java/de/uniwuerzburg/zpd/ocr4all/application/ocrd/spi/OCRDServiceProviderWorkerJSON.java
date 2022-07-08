/**
 * File:     OCRDServiceProviderWorkerJSON.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     08.07.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi;

import java.security.ProviderException;

/**
 * Defines ocr-d service provider workers with JSON support. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public abstract class OCRDServiceProviderWorkerJSON extends OCRDServiceProviderWorker {
	
	/**
	 * The ocr-d service provider description as json.
	 */
	private String jsonDescription = null;

	/**
	 * Default constructor for an ocr-d service provider worker with JSON support.
	 * 
	 * @since 1.8
	 */
	public OCRDServiceProviderWorkerJSON() {
		super();
	}

	/**
	 * Creates an ocr-d service provider worker with JSON support.
	 * 
	 * @param resourceBundleKeyPrefix The prefix of the keys in the resource bundle.
	 * @since 1.8
	 */
	public OCRDServiceProviderWorkerJSON(String resourceBundleKeyPrefix) {
		super(resourceBundleKeyPrefix);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getProvider(
	 * )
	 */
	@Override
	public String getProvider() {
		return super.getProvider() + "/json";
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProviderCore#
	 * initializeCallback()
	 */
	@Override
	public void initializeCallback() throws ProviderException {
		// TODO LOAD JSON
		
	}

}
