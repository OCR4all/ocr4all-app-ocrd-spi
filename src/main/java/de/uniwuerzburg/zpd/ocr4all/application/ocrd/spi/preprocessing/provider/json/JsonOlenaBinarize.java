/**
 * File:     JsonOlenaBinarize.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.preprocessing.provider.json
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     09.05.2023
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.preprocessing.provider.json;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.JsonOCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.PreprocessingServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;

/**
 * Defines service providers for ocr-d Olena/SCRIBO binarize with JSON support.
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
 * <li>olena-binarize-json-id: ocrd-olena-binarize</li>
 * <li>olena-binarize-json-description: ocr-d Olena/SCRIBO binarize processor
 * </li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class JsonOlenaBinarize extends JsonOCRDServiceProviderWorker implements PreprocessingServiceProvider {
	/**
	 * The service provider name;
	 */
	private static final String name = "Olena/SCRIBO binarize";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("olena-binarize-json-id", "ocrd-olena-binarize"),
		processorDescription("olena-binarize-json-description", "ocr-d Olena/SCRIBO binarize processor");

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
		 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework.
		 * ServiceProviderCollectionKey#getName()
		 */
		@Override
		public String getName() {
			return collectionName;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework.
		 * ServiceProviderCollectionKey#getKey()
		 */
		@Override
		public String getKey() {
			return key;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework.
		 * ServiceProviderCollectionKey#getDefaultValue()
		 */
		@Override
		public String getDefaultValue() {
			return defaultValue;
		}
	}

	/**
	 * Default constructor for a service provider for ocr-d Olena/SCRIBO binarize
	 * with JSON support.
	 * 
	 * @since 1.8
	 */
	public JsonOlenaBinarize() {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker#
	 * processorIdentifier()
	 */
	@Override
	protected ConfigurationServiceProvider.CollectionKey processorIdentifier() {
		return ServiceProviderCollection.processorIdentifier;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker#
	 * processorDescription()
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
		return 1500;
	}

}
