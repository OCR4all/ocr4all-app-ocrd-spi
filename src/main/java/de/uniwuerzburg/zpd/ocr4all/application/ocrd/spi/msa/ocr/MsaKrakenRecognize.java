/**
 * File:     MsaKrakenRecognize.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.ocr
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     31.07.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.OCRDMsaServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalCharacterRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessorCore;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ProcessFramework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Field;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.Argument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.SelectArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.StringArgument;

/**
 * Defines service providers for the ocr-d microservice architecture (MSA) of
 * the Kraken recognize processor. The following properties of the service
 * provider collection <b>ocr-d</b> override the local default settings
 * (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>msa-kraken-recognize-id: ocrd-kraken-recognize</li>
 * <li>msa-kraken-recognize-description: ocr-d Kraken recognize processor</li>
 * <li>msa-kraken-recognize-default-model: «null»</li>
 * <li>see {@link OCRDMsaServiceProviderWorker} for remainder settings</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public class MsaKrakenRecognize extends OCRDMsaServiceProviderWorker
		implements OpticalCharacterRecognitionServiceProvider {
	/**
	 * The Tesseract default model extension.
	 */
	private static final String defaultModelExtension = "mlmodel";

	/**
	 * The model argument.
	 */
	private static final String modelArgument = "model";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	protected enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("msa-kraken-recognize-id", "ocrd-kraken-recognize"),
		processorDescription("msa-kraken-recognize-description", "ocr-d Kraken recognize processor"),
		defaultModel("msa-kraken-recognize-default-model", null);

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
	 * architecture (MSA) of the Kraken recognize processor.
	 * 
	 * @since 17
	 */
	public MsaKrakenRecognize() {
		this(MsaKrakenRecognize.class);
	}

	/**
	 * Creates a service providers for the ocr-d microservice architecture (MSA) of
	 * the Kraken recognize processor.
	 * 
	 * @param logger The logger class.
	 * @since 17
	 */
	protected MsaKrakenRecognize(Class<?> logger) {
		super(logger);
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
		return 3100;
	}

	/**
	 * Returns the Tesserocr models. The hidden file names in the opt models path,
	 * this means staring with a dot, are ignored.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @return The Calamari models.
	 * @since 1.8
	 */
	private List<String> getModels(ConfigurationServiceProvider configuration, Target target) {
		List<String> models = new ArrayList<>();

		try {
			for (Path path : getFilesTopLevelFolder(
					getOptResources(configuration, target, ServiceProviderCollection.processorIdentifier),
					defaultModelExtension)) {
				String model = path.getFileName().toString();

				// Removes from model name the default extension
				if (!model.startsWith("."))
					models.add(model.substring(0, model.length() - defaultModelExtension.length() - 1));
			}
		} catch (IOException e) {
			// Nothing to do
		}

		Collections.sort(models, String.CASE_INSENSITIVE_ORDER);

		return models;
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
		return getModels(configuration, target).isEmpty() ? new Premise(Premise.State.warn,
				locale -> "There are no models available in the ocr-d opt directory '"
						+ getOptResources(configuration, target, ServiceProviderCollection.processorIdentifier)
								.toString()
						+ "'.")
				: super.getPremise(target);
	}

	/**
	 * Returns the default model. Extending classes can overwrite this method to set
	 * a default model.
	 * 
	 * @return The default model. Null, if there is no default model.
	 * @since 1.8
	 */
	protected ConfigurationServiceProvider.CollectionKey getDefaultModel() {
		return ServiceProviderCollection.defaultModel;
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
			ProviderDescription.ModelFactory.ModelFieldCallback modelsCallback = new ProviderDescription.ModelFactory.ModelFieldCallback() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription.
				 * ModelFactory.ModelFieldCallback#handle(de.uniwuerzburg.zpd.ocr4all.
				 * application.spi.model.Field)
				 */
				@Override
				public List<Field<?>> handle(Field<?> field) {
					if (field instanceof StringField) {
						final StringField stringField = ((StringField) field);

						String value = ConfigurationServiceProvider.getValue(configuration, getDefaultModel());
						if (value == null)
							value = stringField.getValue().orElse(null);

						if (value != null && value.endsWith("." + defaultModelExtension))
							value = value.substring(0, value.length() - defaultModelExtension.length() - 1);

						final List<SelectField.Item> models = new ArrayList<SelectField.Item>();
						for (String model : getModels(configuration, target))
							models.add(new SelectField.Option(model.equals(value), model, null));

						if (models.isEmpty())
							models.add(new SelectField.Option(false, "empty", locale -> "model.empty"));

						return Arrays.asList(new SelectField[] { new SelectField(stringField.getArgument(),
								locale -> stringField.getLabel(locale),
								locale -> stringField.getDescription(locale).orElse(null), false, models, false) });
					} else
						return null;
				}
			};

			Hashtable<String, ProviderDescription.ModelFactory.ModelFieldCallback> callbacks = new Hashtable<>();
			callbacks.put(modelArgument, modelsCallback);

			return callbacks;
		} else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.
	 * OCRDMsaServiceProviderWorker#getProcessorCallbacks(de.uniwuerzburg.zpd.
	 * ocr4all.application.spi.core.CoreProcessorServiceProvider, java.util.List)
	 */
	@Override
	protected Hashtable<String, ProviderDescription.ModelFactory.ModelArgumentCallback> getProcessorCallbacks(
			CoreProcessorServiceProvider<ProcessorCore.LockSnapshotCallback, ProcessFramework> processor,
			List<String> arguments) {
		if (arguments.contains(modelArgument)) {
			ProviderDescription.ModelFactory.ModelArgumentCallback modelsCallback = new ProviderDescription.ModelFactory.ModelArgumentCallback() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription.
				 * ModelFactory.ModelArgumentCallback#handle(de.uniwuerzburg.zpd.ocr4all.
				 * application.spi.model.argument.Argument, java.util.Set)
				 */
				@Override
				public List<Argument> handle(Argument argument, Set<String> jsonTypeObjectProcessorParameters) {
					if (argument instanceof SelectArgument) {
						SelectArgument selectArgument = (SelectArgument) argument;

						// a single model is expected
						if (selectArgument.getValues().isPresent())
							return Arrays.asList(new StringArgument[] { new StringArgument(selectArgument.getArgument(),
									selectArgument.getValues().get().get(0) + "." + defaultModelExtension) });
					}

					return null;
				}
			};

			Hashtable<String, ProviderDescription.ModelFactory.ModelArgumentCallback> callbacks = new Hashtable<>();
			callbacks.put(modelArgument, modelsCallback);

			return callbacks;
		} else
			return null;
	}

}
