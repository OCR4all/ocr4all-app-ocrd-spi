/**
 * File:     CISOcropySegment.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.olr.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     06.04.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.olr.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalLayoutRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.BooleanField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.DecimalField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.IntegerField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.BooleanArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.DecimalArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.IntegerArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.SelectArgument;

/**
 * Defines service providers for ocr-d cis ocropy segment. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * <li>cis-ocropy-segment-id: ocrd-cis-ocropy-segment</li>
 * <li>cis-ocropy-segment-description: ocr-d cis ocropy segment processor</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class CISOcropySegment extends OCRDServiceProviderWorker implements OpticalLayoutRecognitionServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "olr.cis.ocropy.segment.";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements Framework.ServiceProviderCollectionKey {
		processorIdentifier("cis-ocropy-segment-id", "ocrd-cis-ocropy-segment"),
		processorDescription("cis-ocropy-segment-description", "ocr-d cis ocropy segment processor");

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
		dpi, levelOperation("level-of-operation"), maxColSeps("maximum-column-separators"),
		maxSeps("maximum-number-column-separators"), maxImages("maximum-images"),
		csMinHeight("minimum-height-column-separators"), hlMinWidth("minimum-width-horizontal-separators"),
		gapHeight("gap-height"), gapWidth("gap-width"), overwriteOrder("overwrite-order"),
		overwriteSeparators("overwrite-separators"), overwriteRegions("overwrite-regions"),
		overwriteLines("overwrite-lines"), spread;

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
	 * Defines level of operations.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum LevelOperation {
		page, table, region;

		/**
		 * The default level.
		 */
		public static LevelOperation defaultLevel = region;

		/**
		 * Returns the level of operation with given name.
		 * 
		 * @param name The level name.
		 * @return The level with given name. Null if unknown.
		 * @since 1.8
		 */
		public static LevelOperation getLevel(String name) {
			if (name != null && !name.isBlank()) {
				name = name.trim();

				for (LevelOperation level : LevelOperation.values())
					if (level.name().equals(name))
						return level;
			}

			return null;
		}

	}

	/**
	 * Default constructor for a service provider for ocr-d cis ocropy segment.
	 * 
	 * @since 1.8
	 */
	public CISOcropySegment() {
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
	protected Framework.ServiceProviderCollectionKey processorIdentifier() {
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
	protected Framework.ServiceProviderCollectionKey processorDescription() {
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
		return configuration.isSystemCommandAvailable(SystemCommand.Type.docker) ? new Premise()
				: new Premise(Premise.State.block, locale -> getMessage(locale, "no.command.docker"));
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
		ProcessorArgumentLevelOperationPage argument = new ProcessorArgumentLevelOperationPage();

		final List<SelectField.Item> levelOperations = new ArrayList<SelectField.Item>();
		for (LevelOperation levelOperation : LevelOperation.values())
			levelOperations.add(
					new SelectField.Option(levelOperation.equals(argument.getLevelOperation()), levelOperation.name(),
							locale -> getMessage(locale, "page.xml.level.operation." + levelOperation.name())));

		return new Model(
				new IntegerField(Field.dpi.getName(), argument.getDpi(), locale -> getString(locale, "dpi"),
						locale -> getString(locale, "dpi.description"), null, 1, -1, null, locale -> "pt", false),
				new SelectField(Field.levelOperation.getName(),
						locale -> getMessage(locale, "page.xml.level.operation"),
						locale -> getString(locale, "level.operation.description"), false, levelOperations, false),
				new IntegerField(Field.maxColSeps.getName(), argument.getMaxColSeps(),
						locale -> getString(locale, "maximum.column.separators"),
						locale -> getString(locale, "maximum.column.separators.description"), null, 1, null, null, null,
						false),
				new IntegerField(Field.maxSeps.getName(), argument.getMaxSeps(),
						locale -> getString(locale, "maximum.number.column.separators"),
						locale -> getString(locale, "maximum.number.column.separators.description"), null, 1, null,
						null, null, false),
				new IntegerField(Field.maxImages.getName(), argument.getMaxImages(),
						locale -> getString(locale, "maximum.images"),
						locale -> getString(locale, "maximum.images.description"), null, 1, null, null, null, false),
				new IntegerField(Field.csMinHeight.getName(), argument.getCsMinHeight(),
						locale -> getString(locale, "minimum.height.column.separators"),
						locale -> getString(locale, "minimum.height.column.separators.description"), null, 1, null,
						null, null, false),
				new IntegerField(Field.hlMinWidth.getName(), argument.getHlMinWidth(),
						locale -> getString(locale, "minimum.width.horizontal.separators"),
						locale -> getString(locale, "minimum.width.horizontal.separators.description"), null, 1, null,
						null, null, false),
				new DecimalField(Field.gapHeight.getName(), argument.getGapHeight(),
						locale -> getString(locale, "gap.height"),
						locale -> getString(locale, "gap.height.description"), null, null, null, null, locale -> "px",
						false),
				new DecimalField(Field.gapWidth.getName(), argument.getGapWidth(),
						locale -> getString(locale, "gap.width"), locale -> getString(locale, "gap.width.description"),
						null, null, null, null, null, false),
				new BooleanField(Field.overwriteOrder.getName(), argument.isOverwriteOrder(),
						locale -> getString(locale, "overwrite.order"),
						locale -> getString(locale, "overwrite.order.description"), false),
				new BooleanField(Field.overwriteSeparators.getName(), argument.isOverwriteSeparators(),
						locale -> getString(locale, "overwrite.separators"),
						locale -> getString(locale, "overwrite.separators.description"), false),
				new BooleanField(Field.overwriteRegions.getName(), argument.isOverwriteRegions(),
						locale -> getString(locale, "overwrite.regions"),
						locale -> getString(locale, "overwrite.regions.description"), false),
				new BooleanField(Field.overwriteLines.getName(),
						(new ProcessorArgumentLevelOperationRegion()).isOverwriteLines(),
						locale -> getString(locale, "overwrite.lines"),
						locale -> getString(locale, "overwrite.lines.description"), false),
				new DecimalField(Field.spread.getName(), argument.getSpread(), locale -> getString(locale, "spread"),
						locale -> getString(locale, "spread.description"), null, null, null, null, locale -> "pt",
						false));
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
				if (!initialize(getProcessorIdentifier(framework), callback, framework))
					return ProcessServiceProvider.Processor.State.canceled;

				/*
				 * Available arguments
				 */
				Set<String> availableArguments = modelArgument.getArgumentNames();

				updatedStandardOutput("Parse parameters.");

				/*
				 * The arguments depends on level of operation
				 */
				LevelOperation levelOperation = LevelOperation.defaultLevel;

				if (availableArguments.remove(Field.levelOperation.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.levelOperation.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								levelOperation = LevelOperation.getLevel(values.get(0));

								if (levelOperation == null) {
									updatedStandardError(
											"The level of operation "
													+ (values.get(0) == null || values.get(0).isBlank() ? ""
															: " '" + values.get(0).trim() + "'")
													+ " is not supported.");

									return ProcessServiceProvider.Processor.State.interrupted;
								}
							} else if (values.size() > 1) {
								updatedStandardError("Only one level of operation can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.levelOperation.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Processor arguments
				 */
				ProcessorArgument processorArgument;

				switch (levelOperation) {
				case table:
					processorArgument = new ProcessorArgumentLevelOperationPageTable();
					break;
				case page:
					processorArgument = new ProcessorArgumentLevelOperationPage();
					break;
				case region:
					processorArgument = new ProcessorArgumentLevelOperationRegion();
					break;
				default:
					processorArgument = new ProcessorArgument();
					break;

				}

				processorArgument.setLevelOperation(levelOperation);

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
				 * Spread parameter
				 */
				if (availableArguments.remove(Field.spread.getName()))
					try {
						final DecimalArgument argument = modelArgument.getArgument(DecimalArgument.class,
								Field.spread.getName());

						if (argument.getValue().isPresent())
							processorArgument.setSpread(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.spread.getName() + "' is not of decimal type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				if (processorArgument instanceof ProcessorArgumentLevelOperationPageTable) {
					ProcessorArgumentLevelOperationPageTable processorArgumentLevelOperationPageTable = (ProcessorArgumentLevelOperationPageTable) processorArgument;

					/*
					 * Maximum column separators parameter
					 */
					if (availableArguments.remove(Field.maxColSeps.getName()))
						try {
							final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
									Field.maxColSeps.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setMaxColSeps(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.maxColSeps.getName() + "' is not of integer type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Maximum number column separators parameter
					 */
					if (availableArguments.remove(Field.maxSeps.getName()))
						try {
							final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
									Field.maxSeps.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setMaxSeps(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.maxSeps.getName() + "' is not of integer type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Minimum height column separators parameter
					 */
					if (availableArguments.remove(Field.csMinHeight.getName()))
						try {
							final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
									Field.csMinHeight.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setCsMinHeight(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.csMinHeight.getName() + "' is not of integer type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Minimum width horizontal separators parameter
					 */
					if (availableArguments.remove(Field.hlMinWidth.getName()))
						try {
							final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
									Field.hlMinWidth.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setHlMinWidth(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.hlMinWidth.getName() + "' is not of integer type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Gap height parameter
					 */
					if (availableArguments.remove(Field.gapHeight.getName()))
						try {
							final DecimalArgument argument = modelArgument.getArgument(DecimalArgument.class,
									Field.gapHeight.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setGapHeight(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.gapHeight.getName() + "' is not of decimal type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Gap width parameter
					 */
					if (availableArguments.remove(Field.gapWidth.getName()))
						try {
							final DecimalArgument argument = modelArgument.getArgument(DecimalArgument.class,
									Field.gapWidth.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setGapWidth(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.gapWidth.getName() + "' is not of decimal type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Overwrite order parameter
					 */
					if (availableArguments.remove(Field.overwriteOrder.getName()))
						try {
							final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
									Field.overwriteOrder.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setOverwriteOrder(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.overwriteOrder.getName() + "' is not of boolean type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Overwrite separators parameter
					 */
					if (availableArguments.remove(Field.overwriteSeparators.getName()))
						try {
							final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
									Field.overwriteSeparators.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable
										.setOverwriteSeparators(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError("The argument '" + Field.overwriteSeparators.getName()
									+ "' is not of boolean type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					/*
					 * Overwrite regions parameter
					 */
					if (availableArguments.remove(Field.overwriteRegions.getName()))
						try {
							final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
									Field.overwriteRegions.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationPageTable.setOverwriteRegions(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.overwriteRegions.getName() + "' is not of boolean type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					if (processorArgument instanceof ProcessorArgumentLevelOperationPage) {
						ProcessorArgumentLevelOperationPage processorArgumentLevelOperationPage = (ProcessorArgumentLevelOperationPage) processorArgument;

						/*
						 * Maximum images parameter
						 */
						if (availableArguments.remove(Field.maxImages.getName()))
							try {
								final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
										Field.maxImages.getName());

								if (argument.getValue().isPresent())
									processorArgumentLevelOperationPage.setMaxImages(argument.getValue().get());
							} catch (ClassCastException e) {
								updatedStandardError(
										"The argument '" + Field.maxImages.getName() + "' is not of integer type.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
					}
				} else if (processorArgument instanceof ProcessorArgumentLevelOperationRegion) {
					ProcessorArgumentLevelOperationRegion processorArgumentLevelOperationRegion = (ProcessorArgumentLevelOperationRegion) processorArgument;

					/*
					 * Overwrite lines parameter
					 */
					if (availableArguments.remove(Field.overwriteLines.getName()))
						try {
							final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
									Field.overwriteLines.getName());

							if (argument.getValue().isPresent())
								processorArgumentLevelOperationRegion.setOverwriteLines(argument.getValue().get());
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.overwriteLines.getName() + "' is not of boolean type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}
				}

				/*
				 * Runs the processor
				 */
				return run(framework, processorArgument, availableArguments, () -> isCanceled(), () -> complete(),
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
		 * The pixel density.
		 */
		private int dpi = -1;

		/**
		 * The level of operation.
		 */
		@JsonProperty("level-of-operation")
		private LevelOperation levelOperation = LevelOperation.defaultLevel;

		/**
		 * The spread.
		 */
		private float spread = 2.4F;

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
		 * Returns the level of operation.
		 *
		 * @return The level of operation.
		 * @since 1.8
		 */
		public LevelOperation getLevelOperation() {
			return levelOperation;
		}

		/**
		 * Set the level of operation.
		 *
		 * @param levelOperation The level of operation to set.
		 * @since 1.8
		 */
		public void setLevelOperation(LevelOperation levelOperation) {
			this.levelOperation = levelOperation;
		}

		/**
		 * Returns the spread.
		 *
		 * @return The spread.
		 * @since 1.8
		 */
		public float getSpread() {
			return spread;
		}

		/**
		 * Set the spread.
		 *
		 * @param spread The spread to set.
		 * @since 1.8
		 */
		public void setSpread(float spread) {
			this.spread = spread;
		}

	}

	/**
	 * Defines processor arguments with default values for level of operation page
	 * and table.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	public static class ProcessorArgumentLevelOperationPageTable extends ProcessorArgument {
		/**
		 * The maximum column separators.
		 */
		@JsonProperty("maxcolseps")
		private int maxColSeps = 20;

		/**
		 * The maximum number column separators.
		 */
		@JsonProperty("maxseps")
		private int maxSeps = 20;

		/**
		 * The minimum height column separators.
		 */
		@JsonProperty("csminheight")
		private int csMinHeight = 4;

		/**
		 * The minimum width horizontal separators.
		 */
		@JsonProperty("hlminwidth")
		private int hlMinWidth = 10;

		/**
		 * The gap height.
		 */
		@JsonProperty("gap_height")
		private float gapHeight = 0.01F;

		/**
		 * The gap width.
		 */
		@JsonProperty("gap_width")
		private float gapWidth = 1.5F;

		/**
		 * True if overwrite order.
		 */
		@JsonProperty("overwrite_order")
		private boolean isOverwriteOrder = true;

		/**
		 * True if overwrite separators.
		 */
		@JsonProperty("overwrite_separators")
		private boolean isOverwriteSeparators = true;

		/**
		 * True if overwrite regions.
		 */
		@JsonProperty("overwrite_regions")
		private boolean isOverwriteRegions = true;

		/**
		 * Returns the maximum column separators.
		 *
		 * @return The maximum column separators.
		 * @since 1.8
		 */
		public int getMaxColSeps() {
			return maxColSeps;
		}

		/**
		 * Set the maximum column separators.
		 *
		 * @param separators The separators to set.
		 * @since 1.8
		 */
		public void setMaxColSeps(int separators) {
			maxColSeps = separators;
		}

		/**
		 * Returns the maximum number column separators.
		 *
		 * @return The maximum number column separators.
		 * @since 1.8
		 */
		public int getMaxSeps() {
			return maxSeps;
		}

		/**
		 * Set the maximum number column separators.
		 *
		 * @param separators The separators to set.
		 * @since 1.8
		 */
		public void setMaxSeps(int separators) {
			maxSeps = separators;
		}

		/**
		 * Returns the minimum height column separators.
		 *
		 * @return The minimum height column separators.
		 * @since 1.8
		 */
		public int getCsMinHeight() {
			return csMinHeight;
		}

		/**
		 * Set the minimum height column separators.
		 *
		 * @param separators The separators to set.
		 * @since 1.8
		 */
		public void setCsMinHeight(int separators) {
			csMinHeight = separators;
		}

		/**
		 * Returns the minimum width horizontal separators.
		 *
		 * @return The minimum width horizontal separators.
		 * @since 1.8
		 */
		public int getHlMinWidth() {
			return hlMinWidth;
		}

		/**
		 * Set the minimum width horizontal separators.
		 *
		 * @param separators The separators to set.
		 * @since 1.8
		 */
		public void setHlMinWidth(int separators) {
			hlMinWidth = separators;
		}

		/**
		 * Returns the gap height.
		 *
		 * @return The gap height.
		 * @since 1.8
		 */
		public float getGapHeight() {
			return gapHeight;
		}

		/**
		 * Set the gap height.
		 *
		 * @param gap The gap to set.
		 * @since 1.8
		 */
		public void setGapHeight(float gap) {
			gapHeight = gap;
		}

		/**
		 * Returns the gap width.
		 *
		 * @return The gap width.
		 * @since 1.8
		 */
		public float getGapWidth() {
			return gapWidth;
		}

		/**
		 * Set the gap width.
		 *
		 * @param gap The gap to set.
		 * @since 1.8
		 */
		public void setGapWidth(float gap) {
			this.gapWidth = gap;
		}

		/**
		 * Returns true if overwrite order.
		 *
		 * @return True if overwrite order.
		 * @since 1.8
		 */
		@JsonGetter("overwrite_order")
		public boolean isOverwriteOrder() {
			return isOverwriteOrder;
		}

		/**
		 * Set to true if overwrite order.
		 *
		 * @param isOverwrite The overwrite flag to set.
		 * @since 1.8
		 */
		public void setOverwriteOrder(boolean isOverwrite) {
			isOverwriteOrder = isOverwrite;
		}

		/**
		 * Returns true if overwrite separators.
		 *
		 * @return True if overwrite separators.
		 * @since 1.8
		 */
		@JsonGetter("overwrite_separators")
		public boolean isOverwriteSeparators() {
			return isOverwriteSeparators;
		}

		/**
		 * Set to true if overwrite separators.
		 *
		 * @param isOverwrite The overwrite flag to set.
		 * @since 1.8
		 */
		public void setOverwriteSeparators(boolean isOverwrite) {
			isOverwriteSeparators = isOverwrite;
		}

		/**
		 * Returns true if overwrite regions.
		 *
		 * @return True if overwrite regions.
		 * @since 1.8
		 */
		@JsonGetter("overwrite_regions")
		public boolean isOverwriteRegions() {
			return isOverwriteRegions;
		}

		/**
		 * Set to true if overwrite regions.
		 *
		 * @param isOverwrite The overwrite flag to set.
		 * @since 1.8
		 */
		public void setOverwriteRegions(boolean isOverwrite) {
			isOverwriteRegions = isOverwrite;
		}

	}

	/**
	 * Defines processor arguments with default values for level of operation page.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	public static class ProcessorArgumentLevelOperationPage extends ProcessorArgumentLevelOperationPageTable {
		/**
		 * The maximum images.
		 */
		@JsonProperty("maximages")
		private int maxImages = 10;

		/**
		 * Returns the maximum images.
		 *
		 * @return The maximum images.
		 * @since 1.8
		 */
		public int getMaxImages() {
			return maxImages;
		}

		/**
		 * Set the maximum images.
		 *
		 * @param images The images to set.
		 * @since 1.8
		 */
		public void setMaxImages(int images) {
			maxImages = images;
		}
	}

	/**
	 * Defines processor arguments with default values for level of operation
	 * region.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	public static class ProcessorArgumentLevelOperationRegion extends ProcessorArgument {
		/**
		 * True if overwrite lines.
		 */
		@JsonProperty("overwrite_lines")
		private boolean isOverwriteLines = true;

		/**
		 * Returns true if overwrite lines.
		 *
		 * @return True if overwrite lines.
		 * @since 1.8
		 */
		@JsonGetter("overwrite_lines")
		public boolean isOverwriteLines() {
			return isOverwriteLines;
		}

		/**
		 * Set to true if overwrite lines.
		 *
		 * @param isOverwrite The overwrite flag to set.
		 * @since 1.8
		 */
		public void setOverwriteLines(boolean isOverwrite) {
			isOverwriteLines = isOverwrite;
		}

	}
}