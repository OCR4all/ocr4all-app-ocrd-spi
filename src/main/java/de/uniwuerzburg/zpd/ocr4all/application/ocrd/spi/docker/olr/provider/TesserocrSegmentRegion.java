/**
 * File:     TesserocrSegmentRegion.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.olr.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     11.04.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.olr.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.OCRDProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.OpticalLayoutRecognitionServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.BooleanField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.IntegerField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.BooleanArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.IntegerArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;

/**
 * Defines service providers for ocr-d tesserocr segment region. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * <li>docker-stop-wait-kill-seconds: 2</li>
 * <li>tesserocr-segment-region-id: ocrd-tesserocr-segment-region</li>
 * <li>tesserocr-segment-region-description: ocr-d tesserocr segment region
 * processor</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class TesserocrSegmentRegion extends OCRDServiceProviderWorker
		implements OpticalLayoutRecognitionServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "olr.tesseract.segment.region.";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		processorIdentifier("tesserocr-segment-region-id", "ocrd-tesserocr-segment-region"),
		processorDescription("tesserocr-segment-region-description", "ocr-d tesserocr segment region processor");

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
		dpi, overwriteRegions("overwrite-regions"), padding, shrinkPolygons("shrink-polygons"),
		cropPolygons("crop-polygons"), findTables("find-tables"), findStaves("find-staves"), sparseText("sparse-text");

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
	 * Default constructor for a service provider for ocr-d tesserocr segment
	 * region.
	 * 
	 * @since 1.8
	 */
	public TesserocrSegmentRegion() {
		super(messageKeyPrefix);
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
		return Arrays.asList("Layout analysis");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getSteps()
	 */
	@Override
	public List<String> getSteps() {
		return Arrays.asList("layout/segmentation/region");
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getPremise(
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target)
	 */
	@Override
	public Premise getPremise(Target target) {
		return configuration.isSystemCommandAvailable(SystemCommand.Type.docker) ? new Premise()
				: new Premise(Premise.State.block, locale -> getMessage(locale, "no.command.docker"));
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
		// Use processor argument to set the default values
		ProcessorArgument argument = new ProcessorArgument();

		return new Model(
				new IntegerField(Field.dpi.getName(), argument.getDpi(), locale -> getString(locale, "dpi"),
						locale -> getString(locale, "dpi.description"), null, 1, -1, null, locale -> "pt", false),
				new BooleanField(Field.overwriteRegions.getName(), argument.isOverwriteRegions(),
						locale -> getString(locale, "overwrite.regions"),
						locale -> getString(locale, "overwrite.regions.description"), false),
				new IntegerField(Field.padding.getName(), argument.getPadding(), locale -> getString(locale, "padding"),
						locale -> getString(locale, "padding.description"), null, 1, 0, null, locale -> "px", false),
				new BooleanField(Field.shrinkPolygons.getName(), argument.isShrinkPolygons(),
						locale -> getString(locale, "shrink.polygons"),
						locale -> getString(locale, "shrink.polygons.description"), false),
				new BooleanField(Field.cropPolygons.getName(), argument.isCropPolygons(),
						locale -> getString(locale, "crop.polygons"),
						locale -> getString(locale, "crop.polygons.description"), false),
				new BooleanField(Field.findTables.getName(), argument.isFindTables(),
						locale -> getString(locale, "find.tables"),
						locale -> getString(locale, "find.tables.description"), false),
				new BooleanField(Field.findStaves.getName(), argument.isFindStaves(),
						locale -> getString(locale, "find.staves"),
						locale -> getString(locale, "find.staves.description"), false),
				new BooleanField(Field.sparseText.getName(), argument.isSparseText(),
						locale -> getString(locale, "sparse.text"),
						locale -> getString(locale, "sparse.text.description"), false));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider#
	 * newProcessor()
	 */
	@Override
	public ProcessServiceProvider.Processor newProcessor() {

		return new OCRDProcessorServiceProvider() {
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
				 * Overwrite regions parameter
				 */
				if (availableArguments.remove(Field.overwriteRegions.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.overwriteRegions.getName());

						if (argument.getValue().isPresent())
							processorArgument.setOverwriteRegions(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.overwriteRegions.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Padding parameter
				 */
				if (availableArguments.remove(Field.padding.getName()))
					try {
						final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
								Field.padding.getName());

						if (argument.getValue().isPresent()) {
							processorArgument.setPadding(argument.getValue().get());

							if (processorArgument.getPadding() < 0) {
								updatedStandardError("The padding value " + processorArgument.getPadding()
										+ " can not be negative.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}
					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.padding.getName() + "' is not of integer type.");

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
				 * Crop polygons parameter
				 */
				if (availableArguments.remove(Field.cropPolygons.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.cropPolygons.getName());

						if (argument.getValue().isPresent())
							processorArgument.setCropPolygons(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.cropPolygons.getName() + "' is not of boolean type.");

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
				 * Sparse text parameter
				 */
				if (availableArguments.remove(Field.sparseText.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.sparseText.getName());

						if (argument.getValue().isPresent())
							processorArgument.setSparseText(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.sparseText.getName() + "' is not of boolean type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Runs the processor
				 */
				return run(framework, processorArgument, availableArguments, dockerProcess, () -> isCanceled(),
						() -> complete(), message -> updatedStandardOutput(message),
						message -> updatedStandardError(message), progress -> callback.updatedProgress(progress),
						0.01F);
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
		 * True if overwrite regions.
		 */
		@JsonProperty("overwrite_regions")
		private boolean isOverwriteRegions = true;

		/**
		 * The extend detected region rectangles by this many (true) pixels.
		 */
		private int padding = 0;

		/**
		 * True if shrink polygons.
		 */
		@JsonProperty("shrink_polygons")
		private boolean isShrinkPolygons = false;

		/**
		 * True if crop polygons.
		 */
		@JsonProperty("crop_polygons")
		private boolean isCropPolygons = false;

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
		 * Returns true if crop polygons.
		 *
		 * @return True if crop polygons.
		 * @since 1.8
		 */
		@JsonGetter("crop_polygons")
		public boolean isCropPolygons() {
			return isCropPolygons;
		}

		/**
		 * Set to true if crop polygons.
		 *
		 * @param isCropPolygons The crop polygons flag to set.
		 * @since 1.8
		 */
		public void setCropPolygons(boolean isCropPolygons) {
			this.isCropPolygons = isCropPolygons;
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

	}
}