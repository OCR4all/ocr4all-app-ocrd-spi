/**
 * File:     TesserocrRecognize.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     13.04.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalCharacterRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;

/**
 * Defines service providers for ocr-d tesserocr recognize. The following properties of the service
 * provider collection <b>ocr-d</b> override the local default settings
 * (<b>key</b>: <i>default value</i>):
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
public class TesserocrRecognize extends OCRDServiceProviderWorker implements OpticalCharacterRecognitionServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "ocr.tesseract.recognize.";

	/**
	 * The Calamari default model.
	 */
	private static final String defaultModel = "Fraktur";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements Framework.ServiceProviderCollectionKey {
		models("calamari-models", "calamari/models");

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
	 * Defines fields.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum Field {
		model("model"), voter, levelTextEquivalence("level-text-equivalence"),
		glyphConfidenceCutoff("glyph-confidence-cutoff");

		/**
		 * The name.
		 */
		private final String name;

		/**
		 * Creates a model argument.
		 * 
		 * @param name The name.
		 * @since 1.8
		 */
		private Field() {
			name = this.name();
		}

		/**
		 * Creates a model argument.
		 * 
		 * @param name The name.
		 * @since 1.8
		 */
		private Field(String name) {
			this.name = name;
		}

		/**
		 * Returns the name.
		 *
		 * @return The name.
		 * @since 1.8
		 */
		public String getName() {
			return name;
		}

	}

	/**
	 * Defines level of text equivalence.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum LevelTextEquivalence {
		line, word, glyph;

		/**
		 * The default text equivalence.
		 */
		public static LevelTextEquivalence defaultLevel = line;

		/**
		 * Returns the level of operation with given name.
		 * 
		 * @param name The level name.
		 * @return The level with given name. Null if unknown.
		 * @since 1.8
		 */
		public static LevelTextEquivalence getLevel(String name) {
			if (name != null && !name.isBlank()) {
				name = name.trim();

				for (LevelTextEquivalence level : LevelTextEquivalence.values())
					if (level.name().equals(name))
						return level;
			}

			return null;
		}

	}

	/**
	 * Default constructor for a service provider for ocr-d Calamari.
	 * 
	 * @since 1.8
	 */
	public TesserocrRecognize() {
		super(messageKeyPrefix);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker#
	 * processorName()
	 */
	@Override
	protected String processorName() {
		return "ocrd-tesserocr-recognize";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker#
	 * processorDescription()
	 */
	@Override
	protected String processorDescription() {
		return "ocr-d tesserocr recognize processor";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getName(java
	 * .util.Locale)
	 */
	@Override
	public String getName(Locale locale) {
		return getString(locale, "name");
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
	 * de.uniwuerzburg.zpd.ocr4all.application.core.spi.provider.ServiceProvider#
	 * getDescription(java.util.Locale)
	 */
	@Override
	public Optional<String> getDescription(Locale locale) {
		return Optional.of(getString(locale, "description"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.core.spi.provider.ServiceProvider#
	 * getIcon()
	 */
	@Override
	public Optional<String> getIcon() {
		return Optional.of("fa-regular fa-images");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.core.spi.provider.ServiceProvider#
	 * getIndex()
	 */
	@Override
	public int getIndex() {
		return 150;
	}

	/**
	 * Returns the opt models path.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @return The opt models path.
	 * @since 1.8
	 */
	private Path getOptModelsPath(ConfigurationServiceProvider configuration, Target target) {
		return Paths.get(getOptResources(configuration, target).toString(),
				Framework.getValue(configuration, ServiceProviderCollection.models));
	}

	/**
	 * Returns the Calamari models. The hidden file names in the opt models path,
	 * this means staring with a dot, are ignored.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @return The Calamari models.
	 * @since 1.8
	 */
	private List<String> getModels(ConfigurationServiceProvider configuration, Target target) {
		List<String> models = new ArrayList<>();

		for (Path path : getDirectories(getOptModelsPath(configuration, target)))
			if (!path.getFileName().toString().startsWith("."))
				models.add(path.getFileName().toString());

		return models;
	}

	/**
	 * Returns the docker models path.
	 * 
	 * @param configuration The service provider configuration.
	 * @return The docker models path.
	 * @since 1.8
	 */
	private Path getDockerModelsPath(ConfigurationServiceProvider configuration) {
		return Paths.get(getDockerResources(configuration).toString(),
				Framework.getValue(configuration, ServiceProviderCollection.models));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.ServiceProvider#getPremise(de.
	 * uniwuerzburg.zpd.ocr4all.application.spi.ConfigurationServiceProvider,
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.Target)
	 */
	@Override
	public Premise getPremise(ConfigurationServiceProvider configuration, Target target) {
		return getModels(configuration, target).isEmpty()
				? new Premise(Premise.State.warn,
						locale -> getString(locale, "no.models.available",
								new Object[] { getOptModelsPath(configuration, target).toString() }))
				: (configuration.isSystemCommandAvailable(SystemCommand.Type.docker) ? new Premise()
						: new Premise(Premise.State.block, locale -> getMessage(locale, "no.command.docker")));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.ServiceProvider#getModel(de.
	 * uniwuerzburg.zpd.ocr4all.application.spi.ConfigurationServiceProvider,
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.Target)
	 */
	@Override
	public Model getModel(ConfigurationServiceProvider configuration, Target target) {
		// TODO
		return new Model();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider#
	 * newProcessor()
	 */
	@Override
	public ProcessServiceProvider.Processor newProcessor() {
		return new ProcessServiceProvider.Processor() {
			/**
			 * True if the processor was canceled.
			 */
			private boolean isCanceled = false;

			/**
			 * The callback interface for processor updates.
			 */
			private ProcessServiceProvider.Processor.Callback callback;

			/**
			 * The framework.
			 */
			private Framework framework;

			/**
			 * The processor standard output.
			 */
			private StringBuffer standardOutput = new StringBuffer();

			/**
			 * The processor standard error.
			 */
			private StringBuffer standardError = new StringBuffer();

			/**
			 * Callback method for updated standard output.
			 * 
			 * @param message The message.
			 * @since 1.8
			 */
			private void updatedStandardOutput(String message) {
				standardOutput.append(framework.formatLogMessage(message));

				callback.updatedStandardOutput(standardOutput.toString());
			}

			/**
			 * Callback method for updated standard error.
			 * 
			 * @param message The current message.
			 * @since 1.8
			 */
			private void updatedStandardError(String message) {
				standardError.append(framework.formatLogMessage(message));

				callback.updatedStandardError(standardError.toString());
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * de.uniwuerzburg.zpd.ocr4all.application.spi.ProcessServiceProvider.Processor#
			 * execute(de.uniwuerzburg.zpd.ocr4all.application.spi.ProcessServiceProvider.
			 * Processor.Callback, de.uniwuerzburg.zpd.ocr4all.application.spi.Framework,
			 * de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument)
			 */
			@Override
			public State execute(Callback callback, Framework framework, ModelArgument modelArgument) {
				this.callback = callback;
				this.framework = framework;

				callback.updatedProgress(0);

				updatedStandardOutput("Start spi '" + processorName() + "'.");

				if (isCanceled)
					return ProcessServiceProvider.Processor.State.canceled;

				/*
				 * Available arguments
				 */
				Set<String> availableArguments = modelArgument.getArgumentNames();

				updatedStandardOutput("Parse parameters.");

				/*
				 * Processor arguments
				 */
				ProcessorArgument processorArgument = new ProcessorArgument();

				/*
				 * Runs the processor
				 */
				return run(framework, true, processorArgument, availableArguments, () -> isCanceled,
						message -> updatedStandardOutput(message), message -> updatedStandardError(message),
						progress -> callback.updatedProgress(progress), 0.01F);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * de.uniwuerzburg.zpd.ocr4all.application.spi.ProcessServiceProvider.Processor#
			 * cancel()
			 */
			@Override
			public void cancel() {
				isCanceled = true;
			}

		};
	}

	/**
	 * Defines processor arguments with default values.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	public static class ProcessorArgument {
		/**
		 * The model.
		 */
		private String model = defaultModel;

		/**
		 * Returns the model.
		 *
		 * @return The model.
		 * @since 1.8
		 */
		public String getModel() {
			return model;
		}

		/**
		 * Set the model.
		 *
		 * @param model The model to set.
		 * @since 1.8
		 */
		public void setModel(String model) {
			this.model = model;
		}
	}
}