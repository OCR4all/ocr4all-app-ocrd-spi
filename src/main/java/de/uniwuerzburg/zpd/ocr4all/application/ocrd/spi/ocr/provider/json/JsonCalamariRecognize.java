/**
 * File:     JsonCalamariRecognize.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider.json
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     08.07.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider.json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.JsonOCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalCharacterRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Field;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;

/**
 * Defines service providers for ocr-d Calamari recognize with JSON support.
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
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * <li>calamari-recognize-json-id: ocrd-calamari-recognize</li>
 * <li>calamari-recognize-json-description: ocr-d calamari recognize processor
 * (json)</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class JsonCalamariRecognize extends JsonOCRDServiceProviderWorker
		implements OpticalCharacterRecognitionServiceProvider {
	/**
	 * The service provider name;
	 */
	private static final String name = "Calamari recognize (JSON)";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("calamari-recognize-json-id", "ocrd-calamari-recognize"),
		processorDescription("calamari-recognize-json-description", "ocr-d calamari recognize processor (json)");

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
	 * Default constructor for a service provider for ocr-d Calamari recognize with
	 * JSON support.
	 * 
	 * @since 1.8
	 */
	public JsonCalamariRecognize() {
		super(name, true);
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
		return 1000;
	}

	/**
	 * Returns the Calamari models. The hidden directories in the opt models path,
	 * this means staring with a dot, are ignored.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @return The Calamari models.
	 * @since 1.8
	 */
	private List<String> getModels(ConfigurationServiceProvider configuration, Target target) {
		List<String> models = new ArrayList<>();

		for (Path path : getDirectories(getOptResources(configuration, target)))
			if (!path.getFileName().toString().startsWith("."))
				models.add(path.getFileName().toString());

		Collections.sort(models, String.CASE_INSENSITIVE_ORDER);

		return models;
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
		return getModels(configuration, target).isEmpty()
				? new Premise(Premise.State.warn,
						locale -> "There are no models available in the ocr-d opt directory '"
								+ getOptResources(configuration, target).toString() + "'.")
				: super.getPremise(target);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.
	 * JsonOCRDServiceProviderWorker#getModelCallback(de.uniwuerzburg.zpd.ocr4all.
	 * application.spi.env.Target)
	 */
	@Override
	protected Hashtable<String, ModelFieldCallback> getModelCallbacks(Target target) {
		// The models
		ModelFieldCallback modelsCallback = new ModelFieldCallback() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.
			 * JsonOCRDServiceProviderWorker.ModelFieldCallback#handle(de.uniwuerzburg.zpd.
			 * ocr4all.application.spi.model.Field)
			 */
			@Override
			public Field<?> handle(Field<?> field) {
				if (field instanceof StringField) {
					final StringField stringField = ((StringField) field);
					final String value = stringField.getValue().orElse(null);

					final List<SelectField.Item> models = new ArrayList<SelectField.Item>();
					for (String model : getModels(configuration, target))
						models.add(new SelectField.Option(model.equals(value), model, null));

					if (models.isEmpty())
						models.add(new SelectField.Option(false, "empty", locale -> "model.empty"));

					return new SelectField(stringField.getArgument(), locale -> stringField.getDescription(locale),
							locale -> stringField.getWarning(locale).orElse(null), true, models, false);
				} else
					return null;
			}
		};

		Hashtable<String, ModelFieldCallback> callbacks = new Hashtable<>();
		callbacks.put("checkpoint_dir", modelsCallback);

		return callbacks;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.
	 * JsonOCRDServiceProviderWorker#getProcessorCallbacks(de.uniwuerzburg.zpd.
	 * ocr4all.application.spi.core.CoreProcessorServiceProvider)
	 */
	@Override
	protected Hashtable<String, ModelArgumentCallback> getProcessorCallbacks(CoreProcessorServiceProvider processor) {
		// TODO Auto-generated method stub
		return super.getProcessorCallbacks(processor);
	}

}
