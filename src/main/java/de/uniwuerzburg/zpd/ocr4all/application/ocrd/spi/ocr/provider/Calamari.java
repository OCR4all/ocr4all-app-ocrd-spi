/**
 * File:     Calamari.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     12.04.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalCharacterRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.DecimalField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.DecimalArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.SelectArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.StringArgument;

/**
 * Defines service providers for ocr-d Calamari. This processor only operates on
 * the text line level and so needs a line segmentation (and by extension a
 * binarized image) as its input. The following properties of the service
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
public class Calamari extends OCRDServiceProviderWorker implements OpticalCharacterRecognitionServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "ocr.calamari.";

	/**
	 * The Calamari default model.
	 */
	private static final String defaultModel = "fraktur_historical";

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
	public Calamari() {
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
		return "ocrd-calamari-recognize";
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
		return "ocr-d calamari recognize processor";
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
		return 100;
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
		// Use processor argument to set the default values
		ProcessorArgument argument = processorArgumentFactory(configuration);

		// The models
		final List<SelectField.Item> models = new ArrayList<SelectField.Item>();
		final String dockerModelsPath = getDockerModelsPath(configuration).toString();
		for (String model : getModels(configuration, target)) {
			final String modelPath = Paths.get(dockerModelsPath, model).normalize().toString();

			models.add(new SelectField.Option(modelPath.equals(argument.getModel()), modelPath, locale -> model));
		}

		if (models.isEmpty())
			models.add(new SelectField.Option(false, "empty", locale -> getString(locale, "model.empty")));

		// The level of test equivalences
		final List<SelectField.Item> levelTextEquivalences = new ArrayList<SelectField.Item>();
		for (LevelTextEquivalence levelTextEquivalence : LevelTextEquivalence.values())
			levelTextEquivalences.add(new SelectField.Option(
					levelTextEquivalence.equals(argument.getLevelTextEquivalence()), levelTextEquivalence.name(),
					locale -> getString(locale, "level.text.equivalence." + levelTextEquivalence.name())));

		return new Model(
				new SelectField(Field.model.getName(), locale -> getString(locale, "model"),
						locale -> getString(locale, "model.description"), false, models, false),
				new StringField(Field.voter.getName(), argument.getVoter(), locale -> getString(locale, "voter"),
						locale -> getString(locale, "voter.description"), null, false),
				new SelectField(Field.levelTextEquivalence.getName(),
						locale -> getString(locale, "level.text.equivalence"),
						locale -> getString(locale, "level.text.equivalence.description"), false, levelTextEquivalences,
						false),
				new DecimalField(Field.glyphConfidenceCutoff.getName(), argument.getGlyphConfidenceCutoff(),
						locale -> getString(locale, "glyph.confidence.cutoff"),
						locale -> getString(locale, "glyph.confidence.cutoff.description"), null, null, null, null,
						null, false));
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
				ProcessorArgument processorArgument = processorArgumentFactory(framework.getConfiguration());

				/*
				 * Model parameter
				 */
				if (availableArguments.remove(Field.model.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.model.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1)
								processorArgument.setModel(values.get(0));
							else if (values.size() > 1) {
								updatedStandardError("Only one level of text equivalence can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.model.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * voter algorithm parameter
				 */
				if (availableArguments.remove(Field.voter.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.voter.getName());

						if (argument.getValue().isPresent())
							processorArgument.setVoter(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.voter.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Level of text equivalence argument
				 */
				if (availableArguments.remove(Field.levelTextEquivalence.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.levelTextEquivalence.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								processorArgument.setLevelTextEquivalence(LevelTextEquivalence.getLevel(values.get(0)));

								if (processorArgument.getLevelTextEquivalence() == null) {
									updatedStandardError(
											"The level of text equivalence "
													+ (values.get(0) == null || values.get(0).isBlank() ? ""
															: " '" + values.get(0).trim() + "'")
													+ " is not supported.");

									return ProcessServiceProvider.Processor.State.interrupted;
								}
							} else if (values.size() > 1) {
								updatedStandardError("Only one level of text equivalence can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.levelTextEquivalence.getName()
								+ "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Glyph confidences cutoff parameter
				 */
				if (availableArguments.remove(Field.glyphConfidenceCutoff.getName()))
					try {
						final DecimalArgument argument = modelArgument.getArgument(DecimalArgument.class,
								Field.glyphConfidenceCutoff.getName());

						if (argument.getValue().isPresent())
							processorArgument.setGlyphConfidenceCutoff(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.glyphConfidenceCutoff.getName() + "' is not of decimal type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

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
	 * Returns the processor arguments with default values for given service
	 * provider configuration.
	 * 
	 * @param configuration The service provider configuration.
	 * @return The processor arguments with default values.
	 * @since 1.8
	 */
	private ProcessorArgument processorArgumentFactory(ConfigurationServiceProvider configuration) {
		final Path dockerModelsPath = getDockerModelsPath(configuration);

		// Use processor argument to set the default values
		ProcessorArgument argument = new ProcessorArgument();
		argument.setModel(Paths.get(dockerModelsPath.toString(), defaultModel).normalize().toString());

		return argument;
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
		@JsonProperty("checkpoint_dir")
		private String model = null;

		/**
		 * The voter algorithm.
		 */
		private String voter = "confidence_voter_default_ctc";

		/**
		 * The level of text equivalence.
		 */
		@JsonProperty("textequiv_level")
		private LevelTextEquivalence levelTextEquivalence = LevelTextEquivalence.defaultLevel;

		/**
		 * The glyph confidences cutoff.
		 */
		@JsonProperty("glyph_conf_cutoff")
		private float glyphConfidenceCutoff = 0.001F;

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

		/**
		 * Returns the voter algorithm.
		 *
		 * @return The voter algorithm.
		 * @since 1.8
		 */
		public String getVoter() {
			return voter;
		}

		/**
		 * Set the voter algorithm.
		 *
		 * @param voter The voter algorithm to set.
		 * @since 1.8
		 */
		public void setVoter(String voter) {
			this.voter = voter;
		}

		/**
		 * Returns the level of text equivalence.
		 *
		 * @return The level of text equivalence.
		 * @since 1.8
		 */
		public LevelTextEquivalence getLevelTextEquivalence() {
			return levelTextEquivalence;
		}

		/**
		 * Set the level of text equivalence.
		 *
		 * @param level The level to set.
		 * @since 1.8
		 */
		public void setLevelTextEquivalence(LevelTextEquivalence level) {
			levelTextEquivalence = level;
		}

		/**
		 * Returns the glyph confidences cutoff.
		 *
		 * @return The glyph confidences cutoff.
		 * @since 1.8
		 */
		public float getGlyphConfidenceCutoff() {
			return glyphConfidenceCutoff;
		}

		/**
		 * Set the glyph confidences cutoff.
		 *
		 * @param cutoff The cutoff to set.
		 * @since 1.8
		 */
		public void setGlyphConfidenceCutoff(float cutoff) {
			glyphConfidenceCutoff = cutoff;
		}
	}
}