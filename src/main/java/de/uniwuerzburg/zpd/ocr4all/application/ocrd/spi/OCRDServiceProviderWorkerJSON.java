/**
 * File:     OCRDServiceProviderWorkerJSON.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     08.07.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi;

import java.security.ProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.util.SystemProcess;

/**
 * Defines ocr-d service provider workers with JSON support. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>json: -J</li>
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
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		json("json", "-J");

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
	 * The ocr-d JSON processor description.
	 */
	protected String jsonProcessorDescription = null;

	/**
	 * The service provider description.
	 */
	private String description = null;

	/**
	 * The service provider parameters.
	 */
	private JsonNode parameters = null;

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
		SystemProcess process = getDockerProcess();

		try {
			process.execute(new ArrayList<>(Arrays.asList("run", "--rm", getDockerImage(), getProcessorIdentifier(),
					ConfigurationServiceProvider.getValue(configuration, ServiceProviderCollection.json))));
		} catch (Exception e) {
			throw new ProviderException(e.getMessage());
		}

		if (process.getExitValue() == 0)
			initializeJSON(process.getStandardOutput());
		else
			throw new ProviderException(
					process.getStandardError().trim() + " (process exit code " + process.getExitValue() + ")");
	}

	/**
	 * Initializes the provider using the JSON processor description.
	 * 
	 * @param json The JSON processor description.
	 * @since 1.8
	 */
	private void initializeJSON(String json) throws ProviderException {
		if (!json.isBlank()) {
			jsonProcessorDescription = json.trim();

			ObjectMapper mapper = new ObjectMapper();
			try {
				final JsonNode root = mapper.readTree(jsonProcessorDescription);

				description = getText(root.get("description"));
				parameters = root.get("parameters");

			} catch (JsonProcessingException e) {
				throw new ProviderException("could not parse JSON processor description - " + e.getMessage());
			}
		}
	}

	/**
	 * Returns the JSON text of the given node.
	 * 
	 * @param node The JSON node.
	 * @return The JSON text of the given node. Null if the node is null or it is a
	 *         container.
	 * @since 1.8
	 */
	public static String getText(final JsonNode node) {
		if (node == null || node.isContainerNode())
			return null;
		else {
			return node.asText();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#
	 * getDescription(java.util.Locale)
	 */
	@Override
	public Optional<String> getDescription(Locale locale) {
		return description == null ? super.getDescription(locale) : Optional.of(description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getAdvice()
	 */
	@Override
	public String getAdvice() {
		return jsonProcessorDescription == null ? null : "JSON processor description:\n" + jsonProcessorDescription;
	}

}
