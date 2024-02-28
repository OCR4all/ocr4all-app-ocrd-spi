/**
 * File:     JsonTesserocrFontshape.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.ocr.provider.json
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     10.05.2023
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.ocr.provider.json;

import java.nio.file.Path;

import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;

/**
 * Defines service providers for ocr-d Tesserocr font shape with JSON support.
 * This processor only operates on the text line level and so needs a line
 * segmentation (and by extension a binarized image) as its input. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>json: -J</li>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * <li>docker-stop-wait-kill-seconds: 2</li>
 * <li>tesserocr-fontshape-json-id: ocrd-tesserocr-fontshape</li>
 * <li>tesserocr-fontshape-json-description: ocr-d tesserocr font shape
 * processor</li>
 * <li>tesserocr-fontshape-json-default-model: null</li>
 * <li>tesserocr-docker-resources: /usr/local/share/tessdata</li>
 * <li>tesserocr-recognize-json-id: ocrd-tesserocr-recognize</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class JsonTesserocrFontshape extends JsonTesserocrRecognize {
	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("tesserocr-fontshape-json-id", "ocrd-tesserocr-fontshape"),
		processorDescription("tesserocr-fontshape-json-description", "ocr-d Tesserocr font shape processor"),
		defaultModel("tesserocr-fontshape-json-default-model", null);

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
	 * Default constructor for a service provider for ocr-d Tesserocr font shape
	 * with JSON support.
	 * 
	 * @since 1.8
	 */
	public JsonTesserocrFontshape() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.OCRDServiceProviderWorker#
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
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.OCRDServiceProviderWorker#
	 * processorDescription()
	 */
	@Override
	protected ConfigurationServiceProvider.CollectionKey processorDescription() {
		return ServiceProviderCollection.processorDescription;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.ocr.provider.json.
	 * JsonTesserocrRecognize#getDefaultModel()
	 */
	@Override
	protected ConfigurationServiceProvider.CollectionKey getDefaultModel() {
		return ServiceProviderCollection.defaultModel;
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
		return 1000;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.OCRDServiceProviderWorker#
	 * getOptResources(de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework)
	 */
	@Override
	protected Path getOptResources(Framework framework) {
		return getOptResources(framework, JsonTesserocrRecognize.ServiceProviderCollection.processorIdentifier);
	}

}
