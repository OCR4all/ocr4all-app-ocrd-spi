/**
 * File:     TesserocrRecognize.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     13.04.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.ocr.provider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalCharacterRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.BooleanField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.IntegerField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.BooleanArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.IntegerArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.SelectArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.StringArgument;

/**
 * Defines service providers for ocr-d Tesserocr recognize. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * <li>tesserocr-recognize-id: ocrd-tesserocr-recognize</li>
 * <li>tesserocr-recognize-description: ocr-d tesserocr recognize processor</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class TesserocrRecognize extends OCRDServiceProviderWorker
		implements OpticalCharacterRecognitionServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "ocr.tesseract.recognize.";

	/**
	 * The Tesseract default model.
	 */
	private static final String defaultModel = "Fraktur_GT4HistOCR";

	/**
	 * The Tesseract default model extension.
	 */
	private static final String defaultModelExtension = "traineddata";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("tesserocr-recognize-id", "ocrd-tesserocr-recognize"),
		processorDescription("tesserocr-recognize-description", "ocr-d tesserocr recognize processor");

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
		models, autoModel("auto-model"), tesseractEngine("tesseract-engine"), dpi, padding,
		segmentationLevel("segmentation-level"), textEquivLevel("text-equiv-level"),
		overwriteSegments("overwrite-segments"), overwriteText("overwrite-text"), shrinkPolygons("shrink-polygons"),
		blockPolygons("block-polygons"), findTables("find-tables"), findStaves("find-staves"),
		sparseText("sparse-text"), rawLines("raw-lines"), characterWhiteList("character-white-list"),
		characterBlackList("character-black-list"), characterUnblackList("character-unblack-list"),
		tesseractParameters("tesseract-parameters"), xpathParameters("xpath-parameters"), xpathModel("xpath-model");

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
	 * Defines levels.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum Level {
		region, cell, line, word, glyph, none;

		/**
		 * The default level.
		 */
		public static Level defaultLevel = word;

		/**
		 * Returns the level with given name.
		 * 
		 * @param name The level name.
		 * @return The level with given name. Null if unknown.
		 * @since 1.8
		 */
		public static Level getLevel(String name) {
			if (name != null && !name.isBlank()) {
				name = name.trim();

				for (Level level : Level.values())
					if (level.name().equals(name))
						return level;
			}

			return null;
		}

	}

	/**
	 * Defines Tesseract OCR engines.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum TesseractEngine {
		TESSERACT_ONLY, LSTM_ONLY, TESSERACT_LSTM_COMBINED, DEFAULT;

		/**
		 * The default engine.
		 */
		public static TesseractEngine defaultEngine = DEFAULT;

		/**
		 * Returns the engine with given name.
		 * 
		 * @param name The engine name.
		 * @return The level with given name. Null if unknown.
		 * @since 1.8
		 */
		public static TesseractEngine getEngine(String name) {
			if (name != null && !name.isBlank()) {
				name = name.trim();

				for (TesseractEngine engine : TesseractEngine.values())
					if (engine.name().equals(name))
						return engine;
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
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#
	 * getCategories()
	 */
	@Override
	public List<String> getCategories() {
		return Arrays.asList("Text recognition and optimization");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getSteps()
	 */
	@Override
	public List<String> getSteps() {
		return Arrays.asList("layout/segmentation/region", "layout/segmentation/line", "recognition/text-recognition");
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
		return 200;
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
			for (Path path : getFilesTopLevelFolder(getOptResources(configuration, target), defaultModelExtension)) {
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
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getPremise(
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target)
	 */
	@Override
	public Premise getPremise(Target target) {
		return getModels(configuration, target).isEmpty()
				? new Premise(Premise.State.warn,
						locale -> getString(locale, "no.models.available",
								new Object[] { getOptResources(configuration, target).toString() }))
				: (configuration.isSystemCommandAvailable(SystemCommand.Type.docker) ? new Premise()
						: new Premise(Premise.State.block, locale -> getMessage(locale, "no.command.docker")));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getModel(de.
	 * uniwuerzburg.zpd.ocr4all.application.spi.env.Target)
	 */
	@Override
	public Model getModel(Target target) {
		ProcessorArgument argument = new ProcessorArgument();

		// The models
		final List<SelectField.Item> models = new ArrayList<SelectField.Item>();
		for (String model : getModels(configuration, target))
			models.add(new SelectField.Option(model.equals(argument.getModels()), model, locale -> model));

		if (models.isEmpty())
			models.add(new SelectField.Option(false, "empty", locale -> getString(locale, "models.empty")));

		// The levels
		final List<SelectField.Item> segmentationLevels = new ArrayList<SelectField.Item>();
		final List<SelectField.Item> textEquivLevels = new ArrayList<SelectField.Item>();
		for (Level level : Level.values()) {
			segmentationLevels.add(new SelectField.Option(level.equals(argument.getSegmentationLevel()), level.name(),
					locale -> getString(locale, "level." + level.name())));
			textEquivLevels.add(new SelectField.Option(level.equals(argument.getTextEquivLevel()), level.name(),
					locale -> getString(locale, "level." + level.name())));
		}

		// The Tesseract OCR engines
		final List<SelectField.Item> tesseractEngines = new ArrayList<SelectField.Item>();
		for (TesseractEngine engine : TesseractEngine.values())
			tesseractEngines.add(new SelectField.Option(engine.equals(argument.getTesseractEngine()), engine.name(),
					locale -> getString(locale, "engine." + engine.name())));

		return new Model(
				new SelectField(Field.models.getName(), locale -> getString(locale, "models"),
						locale -> getString(locale, "models.description"), true, models, false),
				new BooleanField(Field.autoModel.getName(), argument.isAutoModel(),
						locale -> getString(locale, "auto.model"),
						locale -> getString(locale, "auto.model.description"), false),
				new SelectField(Field.tesseractEngine.getName(), locale -> getString(locale, "engine"),
						locale -> getString(locale, "engine.description"), false, tesseractEngines, false),
				new IntegerField(Field.dpi.getName(), argument.getDpi(), locale -> getString(locale, "dpi"),
						locale -> getString(locale, "dpi.description"), null, 1, -1, null, locale -> "pt", false),
				new IntegerField(Field.padding.getName(), argument.getPadding(), locale -> getString(locale, "padding"),
						locale -> getString(locale, "padding.description"), null, 1, 0, null, locale -> "px", false),
				new SelectField(Field.segmentationLevel.getName(), locale -> getString(locale, "level.segmentation"),
						locale -> getString(locale, "level.segmentation.description"), false, segmentationLevels,
						false),
				new SelectField(Field.textEquivLevel.getName(), locale -> getString(locale, "level.TextEquiv"),
						locale -> getString(locale, "level.TextEquiv.description"), false, textEquivLevels, false),
				new BooleanField(Field.overwriteSegments.getName(), argument.isOverwriteSegments(),
						locale -> getString(locale, "overwrite.segments"),
						locale -> getString(locale, "overwrite.segments.description"), false),
				new BooleanField(Field.overwriteText.getName(), argument.isOverwriteText(),
						locale -> getString(locale, "overwrite.text"),
						locale -> getString(locale, "overwrite.text.description"), false),
				new BooleanField(Field.shrinkPolygons.getName(), argument.isShrinkPolygons(),
						locale -> getString(locale, "shrink.polygons"),
						locale -> getString(locale, "shrink.polygons.description"), false),
				new BooleanField(Field.blockPolygons.getName(), argument.isBlockPolygons(),
						locale -> getString(locale, "block.polygons"),
						locale -> getString(locale, "block.polygons.description"), false),
				new BooleanField(Field.findTables.getName(), argument.isFindTables(),
						locale -> getString(locale, "find.tables"),
						locale -> getString(locale, "find.tables.description"), false),
				new BooleanField(Field.findStaves.getName(), argument.isFindStaves(),
						locale -> getString(locale, "find.staves"),
						locale -> getString(locale, "find.staves.description"), false),
				new BooleanField(Field.sparseText.getName(), argument.isSparseText(),
						locale -> getString(locale, "sparse.text"),
						locale -> getString(locale, "sparse.text.description"), false),
				new BooleanField(Field.rawLines.getName(), argument.isRawLines(),
						locale -> getString(locale, "raw.lines"), locale -> getString(locale, "raw.lines.description"),
						false),
				new StringField(Field.characterWhiteList.getName(), argument.getCharacterWhiteList(),
						locale -> getString(locale, "character.white.list"),
						locale -> getString(locale, "character.white.list.description"), null, false),
				new StringField(Field.characterBlackList.getName(), argument.getCharacterBlackList(),
						locale -> getString(locale, "character.black.list"),
						locale -> getString(locale, "character.black.list.description"), null, false),
				new StringField(Field.characterUnblackList.getName(), argument.getCharacterUnblackList(),
						locale -> getString(locale, "character.unblack.list"),
						locale -> getString(locale, "character.unblack.list.description"), null, false),
				new StringField(Field.tesseractParameters.getName(), argument.getTesseractParameters(),
						locale -> getString(locale, "tesseract.parameters"),
						locale -> getString(locale, "tesseract.parameters.description"), null, false),
				new StringField(Field.xpathParameters.getName(), argument.getXpathParameters(),
						locale -> getString(locale, "xpath.parameters"),
						locale -> getString(locale, "xpath.parameters.description"), null, false),
				new StringField(Field.xpathModel.getName(), argument.getXpathModel(),
						locale -> getString(locale, "xpath.model"),
						locale -> getString(locale, "xpath.model.description"), null, false));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider#
	 * newProcessor()
	 */
	@Override
	public ProcessServiceProvider.Processor newProcessor() {
		return new CoreProcessorServiceProvider() {
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
				if (!initialize(getProcessorIdentifier(), callback, framework))
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
				 * Model parameter
				 */
				if (availableArguments.remove(Field.models.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.models.getName());
						if (argument.getValues().isPresent()) {
							StringBuffer buffer = new StringBuffer();

							// Multiple models are combined by concatenating with +
							for (String value : argument.getValues().get()) {
								if (buffer.length() > 0)
									buffer.append("+");

								buffer.append(value);
							}

							processorArgument.setModels(buffer.toString());
						}
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.models.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Auto model parameter
				 */
				if (availableArguments.remove(Field.autoModel.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.autoModel.getName());

						if (argument.getValue().isPresent())
							processorArgument.setAutoModel(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.autoModel.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Tesseract OCR engine parameter
				 */
				if (availableArguments.remove(Field.tesseractEngine.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.tesseractEngine.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								TesseractEngine engine = TesseractEngine.getEngine(values.get(0));

								if (engine == null) {
									updatedStandardError(
											"The Tesseract OCR engine "
													+ (values.get(0) == null || values.get(0).isBlank() ? ""
															: " '" + values.get(0).trim() + "'")
													+ " is not supported.");

									return ProcessServiceProvider.Processor.State.interrupted;
								} else
									processorArgument.setTesseractEngine(engine);
							} else if (values.size() > 1) {
								updatedStandardError("Only one Tesseract OCR engine can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.tesseractEngine.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * DPI parameter
				 */
				if (availableArguments.remove(Field.dpi.getName()))
					try {
						final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
								Field.dpi.getName());

						if (argument.getValue().isPresent())
							processorArgument.setDpi(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.dpi.getName() + "' is not of integer type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Padding parameter
				 */
				if (availableArguments.remove(Field.padding.getName()))
					try {
						final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
								Field.padding.getName());

						if (argument.getValue().isPresent())
							processorArgument.setPadding(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.padding.getName() + "' is not of integer type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Segmentation level parameter
				 */
				if (availableArguments.remove(Field.segmentationLevel.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.segmentationLevel.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								Level level = Level.getLevel(values.get(0));

								if (level == null) {
									updatedStandardError(
											"The segmentation level "
													+ (values.get(0) == null || values.get(0).isBlank() ? ""
															: " '" + values.get(0).trim() + "'")
													+ " is not supported.");

									return ProcessServiceProvider.Processor.State.interrupted;
								} else
									processorArgument.setSegmentationLevel(level);
							} else if (values.size() > 1) {
								updatedStandardError("Only one segmentation level can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.segmentationLevel.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * TextEquiv level parameter
				 */
				if (availableArguments.remove(Field.textEquivLevel.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.textEquivLevel.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								Level level = Level.getLevel(values.get(0));

								if (level == null) {
									updatedStandardError(
											"The TextEquiv level "
													+ (values.get(0) == null || values.get(0).isBlank() ? ""
															: " '" + values.get(0).trim() + "'")
													+ " is not supported.");

									return ProcessServiceProvider.Processor.State.interrupted;
								} else
									processorArgument.setTextEquivLevel(level);
							} else if (values.size() > 1) {
								updatedStandardError("Only one TextEquiv level can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.textEquivLevel.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Overwrite segments parameter
				 */
				if (availableArguments.remove(Field.overwriteSegments.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.overwriteSegments.getName());

						if (argument.getValue().isPresent())
							processorArgument.setOverwriteSegments(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.overwriteSegments.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Overwrite text parameter
				 */
				if (availableArguments.remove(Field.overwriteText.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.overwriteText.getName());

						if (argument.getValue().isPresent())
							processorArgument.setOverwriteText(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.overwriteText.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Shrink polygons parameter
				 */
				if (availableArguments.remove(Field.shrinkPolygons.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.shrinkPolygons.getName());

						if (argument.getValue().isPresent())
							processorArgument.setShrinkPolygons(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.shrinkPolygons.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Block polygons parameter
				 */
				if (availableArguments.remove(Field.blockPolygons.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.blockPolygons.getName());

						if (argument.getValue().isPresent())
							processorArgument.setBlockPolygons(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.blockPolygons.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Find tables parameter
				 */
				if (availableArguments.remove(Field.findTables.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.findTables.getName());

						if (argument.getValue().isPresent())
							processorArgument.setFindTables(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.findTables.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Find staves parameter
				 */
				if (availableArguments.remove(Field.findStaves.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.findStaves.getName());

						if (argument.getValue().isPresent())
							processorArgument.setFindStaves(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.findStaves.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Raw lines parameter
				 */
				if (availableArguments.remove(Field.rawLines.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.rawLines.getName());

						if (argument.getValue().isPresent())
							processorArgument.setRawLines(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.rawLines.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Character white list parameter
				 */
				if (availableArguments.remove(Field.characterWhiteList.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.characterWhiteList.getName());

						if (argument.getValue().isPresent())
							processorArgument.setCharacterWhiteList(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.characterWhiteList.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Character black list parameter
				 */
				if (availableArguments.remove(Field.characterBlackList.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.characterBlackList.getName());

						if (argument.getValue().isPresent())
							processorArgument.setCharacterBlackList(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.characterBlackList.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Character unblack list parameter
				 */
				if (availableArguments.remove(Field.characterUnblackList.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.characterUnblackList.getName());

						if (argument.getValue().isPresent())
							processorArgument.setCharacterUnblackList(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.characterUnblackList.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * 'Tesseract parameters' parameter
				 */
				if (availableArguments.remove(Field.tesseractParameters.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.tesseractParameters.getName());

						if (argument.getValue().isPresent())
							processorArgument.setTesseractParameters(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.tesseractParameters.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * 'XPath parameters' parameter
				 */
				if (availableArguments.remove(Field.xpathParameters.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.xpathParameters.getName());

						if (argument.getValue().isPresent())
							processorArgument.setXpathParameters(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.xpathParameters.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * XPath model parameter
				 */
				if (availableArguments.remove(Field.xpathModel.getName()))
					try {
						final StringArgument argument = modelArgument.getArgument(StringArgument.class,
								Field.xpathModel.getName());

						if (argument.getValue().isPresent())
							processorArgument.setXpathModel(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.xpathModel.getName() + "' is not of string type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Runs the processor
				 */
				return run(framework, true, processorArgument, availableArguments, () -> isCanceled(), () -> complete(),
						message -> updatedStandardOutput(message), message -> updatedStandardError(message),
						progress -> callback.updatedProgress(progress), 0.01F);
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
		 * The models.
		 */
		@JsonProperty("model")
		private String models = defaultModel;

		/**
		 * True if auto model.
		 */
		@JsonProperty("auto_model")
		private boolean isAutoModel = false;

		/**
		 * The Tesseract OCR engine.
		 */
		@JsonProperty("oem")
		private TesseractEngine tesseractEngine = TesseractEngine.defaultEngine;

		/**
		 * The pixel density.
		 */
		private int dpi = -1;

		/**
		 * The extend detected region/cell/line/word rectangles by this many (true)
		 * pixels.
		 */
		private int padding = 0;

		/**
		 * The segmentation level.
		 */
		@JsonProperty("segmentation_level")
		private Level segmentationLevel = Level.defaultLevel;

		/**
		 * The TextEquiv level.
		 */
		@JsonProperty("textequiv_level")
		private Level textEquivLevel = Level.defaultLevel;

		/**
		 * True if overwrite segments.
		 */
		@JsonProperty("overwrite_segments")
		private boolean isOverwriteSegments = false;

		/**
		 * True if overwrite text.
		 */
		@JsonProperty("overwrite_text")
		private boolean isOverwriteText = true;

		/**
		 * True if shrink polygons.
		 */
		@JsonProperty("shrink_polygons")
		private boolean isShrinkPolygons = false;

		/**
		 * True if block polygons.
		 */
		@JsonProperty("block_polygons")
		private boolean isBlockPolygons = false;

		/**
		 * True if find tables.
		 */
		@JsonProperty("find_tables")
		private boolean isFindTables = true;

		/**
		 * True if find staves.
		 */
		@JsonProperty("find_staves")
		private boolean isFindStaves = false;

		/**
		 * True if sparse text.
		 */
		@JsonProperty("sparse_text")
		private boolean isSparseText = false;

		/**
		 * True if raw lines.
		 */
		@JsonProperty("raw_lines")
		private boolean isRawLines = false;

		/**
		 * The character white list.
		 */
		@JsonProperty("char_whitelist")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String characterWhiteList = null;

		/**
		 * The character black list.
		 */
		@JsonProperty("char_blacklist")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String characterBlackList = null;

		/**
		 * The character unblack list.
		 */
		@JsonProperty("char_unblacklist")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String characterUnblackList = null;

		/**
		 * The Tesseract parameters.
		 */
		@JsonProperty("tesseract_parameters")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String tesseractParameters = null;

		/**
		 * The xpath parameters.
		 */
		@JsonProperty("xpath_parameters")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String xpathParameters = null;

		/**
		 * The xpath model.
		 */
		@JsonProperty("xpath_model")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String xpathModel = null;

		/**
		 * Returns the models.
		 *
		 * @return The models.
		 * @since 1.8
		 */
		public String getModels() {
			return models;
		}

		/**
		 * Set the models.
		 *
		 * @param models The models to set.
		 * @since 1.8
		 */
		public void setModels(String models) {
			this.models = models;
		}

		/**
		 * Returns true if auto model.
		 *
		 * @return True if auto model.
		 * @since 1.8
		 */
		@JsonGetter("auto_model")
		public boolean isAutoModel() {
			return isAutoModel;
		}

		/**
		 * Set to true if auto model.
		 *
		 * @param isAutoModel The auto model to set.
		 * @since 1.8
		 */
		public void setAutoModel(boolean isAutoModel) {
			this.isAutoModel = isAutoModel;
		}

		/**
		 * Returns the Tesseract OCR engine.
		 *
		 * @return The Tesseract OCR engine.
		 * @since 1.8
		 */
		public TesseractEngine getTesseractEngine() {
			return tesseractEngine;
		}

		/**
		 * Set the Tesseract OCR engine.
		 *
		 * @param tesseractEngine The Tesseract OCR engine to set.
		 * @since 1.8
		 */
		public void setTesseractEngine(TesseractEngine tesseractEngine) {
			this.tesseractEngine = tesseractEngine;
		}

		/**
		 * Returns the pixel density.
		 *
		 * @return The pixel density.
		 * @since 1.8
		 */
		public int getDpi() {
			return dpi;
		}

		/**
		 * Set the pixel density. If the pixel density is negative, it is set to -1.
		 *
		 * @param dpi The pixel density to set.
		 * @since 1.8
		 */
		public void setDpi(int dpi) {
			if (dpi >= 0)
				this.dpi = dpi;
			else
				this.dpi = -1;
		}

		/**
		 * Returns the extend detected region rectangles by this many (true) pixels.
		 *
		 * @return The extend detected region rectangles by this many (true) pixels.
		 * @since 1.8
		 */
		public int getPadding() {
			return padding;
		}

		/**
		 * Set the extend detected region rectangles by this many (true) pixels.
		 *
		 * @param padding The padding to set.
		 * @since 1.8
		 */
		public void setPadding(int padding) {
			this.padding = padding;
		}

		/**
		 * Returns the segmentation level.
		 *
		 * @return The segmentation level.
		 * @since 1.8
		 */
		public Level getSegmentationLevel() {
			return segmentationLevel;
		}

		/**
		 * Set the segmentation level.
		 *
		 * @param segmentationLevel The segmentation level to set.
		 * @since 1.8
		 */
		public void setSegmentationLevel(Level segmentationLevel) {
			this.segmentationLevel = segmentationLevel;
		}

		/**
		 * Returns the TextEquiv level.
		 *
		 * @return The TextEquiv level.
		 * @since 1.8
		 */
		public Level getTextEquivLevel() {
			return textEquivLevel;
		}

		/**
		 * Set the TextEquiv level.
		 *
		 * @param textEquivLevel The TextEquiv level to set.
		 * @since 1.8
		 */
		public void setTextEquivLevel(Level textEquivLevel) {
			this.textEquivLevel = textEquivLevel;
		}

		/**
		 * Returns true if overwrite segments.
		 *
		 * @return True if overwrite segments.
		 * @since 1.8
		 */
		@JsonGetter("overwrite_segments")
		public boolean isOverwriteSegments() {
			return isOverwriteSegments;
		}

		/**
		 * Set to true if overwrite segments.
		 *
		 * @param isOverwriteSegments The overwrite segments flag to set.
		 * @since 1.8
		 */
		public void setOverwriteSegments(boolean isOverwriteSegments) {
			this.isOverwriteSegments = isOverwriteSegments;
		}

		/**
		 * Returns true if overwrite text.
		 *
		 * @return True if overwrite text.
		 * @since 1.8
		 */
		@JsonGetter("overwrite_text")
		public boolean isOverwriteText() {
			return isOverwriteText;
		}

		/**
		 * Set to true if overwrite text.
		 *
		 * @param isOverwriteText The overwrite text flag to set.
		 * @since 1.8
		 */
		public void setOverwriteText(boolean isOverwriteText) {
			this.isOverwriteText = isOverwriteText;
		}

		/**
		 * Returns true if shrink polygons.
		 *
		 * @return True if shrink polygons.
		 * @since 1.8
		 */
		@JsonGetter("shrink_polygons")
		public boolean isShrinkPolygons() {
			return isShrinkPolygons;
		}

		/**
		 * Set to true if shrink polygons.
		 *
		 * @param isShrinkPolygons The shrink polygons flag to set.
		 * @since 1.8
		 */
		public void setShrinkPolygons(boolean isShrinkPolygons) {
			this.isShrinkPolygons = isShrinkPolygons;
		}

		/**
		 * Returns true if block polygons.
		 *
		 * @return True if block polygons.
		 * @since 1.8
		 */
		@JsonGetter("block_polygons")
		public boolean isBlockPolygons() {
			return isBlockPolygons;
		}

		/**
		 * Set to true if block polygons.
		 *
		 * @param isBlockPolygons The block polygons flag to set.
		 * @since 1.8
		 */
		public void setBlockPolygons(boolean isBlockPolygons) {
			this.isBlockPolygons = isBlockPolygons;
		}

		/**
		 * Returns true if find tables.
		 *
		 * @return True if find tables.
		 * @since 1.8
		 */
		@JsonGetter("find_tables")
		public boolean isFindTables() {
			return isFindTables;
		}

		/**
		 * Set to true if find tables.
		 *
		 * @param isFindTables The find tables flag to set.
		 * @since 1.8
		 */
		public void setFindTables(boolean isFindTables) {
			this.isFindTables = isFindTables;
		}

		/**
		 * Returns true if find staves.
		 *
		 * @return True if find staves.
		 * @since 1.8
		 */
		@JsonGetter("find_staves")
		public boolean isFindStaves() {
			return isFindStaves;
		}

		/**
		 * Set to true if find staves.
		 *
		 * @param isFindStaves The find staves flag to set.
		 * @since 1.8
		 */
		public void setFindStaves(boolean isFindStaves) {
			this.isFindStaves = isFindStaves;
		}

		/**
		 * Returns true if sparse text.
		 *
		 * @return True if sparse text.
		 * @since 1.8
		 */
		@JsonGetter("sparse_text")
		public boolean isSparseText() {
			return isSparseText;
		}

		/**
		 * Set to true if sparse text.
		 *
		 * @param isSparseText The sparse text flag to set.
		 * @since 1.8
		 */
		public void setSparseText(boolean isSparseText) {
			this.isSparseText = isSparseText;
		}

		/**
		 * Returns true if raw lines.
		 *
		 * @return True if raw lines.
		 * @since 1.8
		 */
		@JsonGetter("raw_lines")
		public boolean isRawLines() {
			return isRawLines;
		}

		/**
		 * Set to true if raw lines.
		 *
		 * @param isRawLines The raw lines flag to set.
		 * @since 1.8
		 */
		public void setRawLines(boolean isRawLines) {
			this.isRawLines = isRawLines;
		}

		/**
		 * Returns the character white list.
		 *
		 * @return The character white list.
		 * @since 1.8
		 */
		public String getCharacterWhiteList() {
			return characterWhiteList;
		}

		/**
		 * Set the character white list.
		 *
		 * @param characterWhiteList The character white list to set.
		 * @since 1.8
		 */
		public void setCharacterWhiteList(String characterWhiteList) {
			this.characterWhiteList = characterWhiteList;
		}

		/**
		 * Returns the character black list.
		 *
		 * @return The character black list.
		 * @since 1.8
		 */
		public String getCharacterBlackList() {
			return characterBlackList;
		}

		/**
		 * Set the character black list.
		 *
		 * @param characterBlackList The character black list to set.
		 * @since 1.8
		 */
		public void setCharacterBlackList(String characterBlackList) {
			this.characterBlackList = characterBlackList;
		}

		/**
		 * Returns the character unblack list.
		 *
		 * @return The character unblack list.
		 * @since 1.8
		 */
		public String getCharacterUnblackList() {
			return characterUnblackList;
		}

		/**
		 * Set the character unblack list.
		 *
		 * @param characterUnblackList The character unblack list to set.
		 * @since 1.8
		 */
		public void setCharacterUnblackList(String characterUnblackList) {
			this.characterUnblackList = characterUnblackList;
		}

		/**
		 * Returns the Tesseract parameters.
		 *
		 * @return The Tesseract parameters.
		 * @since 1.8
		 */
		public String getTesseractParameters() {
			return tesseractParameters;
		}

		/**
		 * Set the Tesseract parameters.
		 *
		 * @param tesseractParameters The Tesseract parameters to set.
		 * @since 1.8
		 */
		public void setTesseractParameters(String tesseractParameters) {
			this.tesseractParameters = tesseractParameters;
		}

		/**
		 * Returns the xpath parameters.
		 *
		 * @return The xpath parameters.
		 * @since 1.8
		 */
		public String getXpathParameters() {
			return xpathParameters;
		}

		/**
		 * Set the xpath parameters.
		 *
		 * @param xpathParameters The xpath parameters to set.
		 * @since 1.8
		 */
		public void setXpathParameters(String xpathParameters) {
			this.xpathParameters = xpathParameters;
		}

		/**
		 * Returns the xpath model.
		 *
		 * @return The xpath model.
		 * @since 1.8
		 */
		public String getXpathModel() {
			return xpathModel;
		}

		/**
		 * Set the xpath model.
		 *
		 * @param xpathModel The xpath model to set.
		 * @since 1.8
		 */
		public void setXpathModel(String xpathModel) {
			this.xpathModel = xpathModel;
		}

	}
}