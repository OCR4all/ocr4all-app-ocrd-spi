/**
 * File:     MsaCISOcropyResegment.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.olr
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     01.08.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.olr;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.OCRDMsaServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalLayoutRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;

/**
 * Defines service providers for the ocr-d microservice architecture (MSA) of
 * the cis ocropy resegment processor. The following properties of the service
 * provider collection <b>ocr-d</b> override the local default settings
 * (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>msa-cis-ocropy-resegment-id: ocrd-cis-ocropy-resegment</li>
 * <li>msa-cis-ocropy-resegment-description: ocr-d cis ocropy resegment
 * processor</li>
 * <li>see {@link OCRDMsaServiceProviderWorker} for remainder settings</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public class MsaCISOcropyResegment extends OCRDMsaServiceProviderWorker
		implements OpticalLayoutRecognitionServiceProvider {
	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("msa-cis-ocropy-resegment-id", "ocrd-cis-ocropy-resegment"),
		processorDescription("msa-cis-ocropy-resegment-description", "ocr-d cis ocropy resegment processor");

		/**
		 * The key.
		 */
		private final String key;

		/**
		 * The default value.
		 */
		private final String defaultValue;

		/**
		 * Creates a service provider collection with a key and default value.
		 * 
		 * @param key          The key.
		 * @param defaultValue The default value.
		 * @since 1.8
		 */
		private ServiceProviderCollection(String key, String defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider.
		 * CollectionKey#getName()
		 */
		@Override
		public String getName() {
			return collectionName;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider.
		 * CollectionKey#getKey()
		 */
		@Override
		public String getKey() {
			return key;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider.
		 * CollectionKey#getDefaultValue()
		 */
		@Override
		public String getDefaultValue() {
			return defaultValue;
		}
	}

	/**
	 * Default constructor for a service providers for the ocr-d microservice
	 * architecture (MSA) of the cis ocropy resegment processor.
	 * 
	 * @since 17
	 */
	public MsaCISOcropyResegment() {
		super(MsaCISOcropyResegment.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core.
	 * OCRDServiceProviderWorker#processorIdentifier()
	 */
	@Override
	protected ConfigurationServiceProvider.CollectionKey processorIdentifier() {
		return ServiceProviderCollection.processorIdentifier;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core.
	 * OCRDServiceProviderWorker#processorDescription()
	 */
	@Override
	protected ConfigurationServiceProvider.CollectionKey processorDescription() {
		return ServiceProviderCollection.processorDescription;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getVersion()
	 */
	@Override
	public float getVersion() {
		return 1.0F;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getIndex()
	 */
	@Override
	public int getIndex() {
		return 2010;
	}

}
