/**
 * File:     TesserocrSegmentLine.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.olr.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     12.04.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.olr.provider;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker;
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
 * Defines service providers for ocr-d tesserocr segment line. The following
 * properties of the service provider collection <b>ocr-d</b> override the local
 * default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>docker-image: ocrd/all:maximum</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public class TesserocrSegmentLine extends OCRDServiceProviderWorker implements OpticalLayoutRecognitionServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "olr.tesseract.segment.line.";

	/**
	 * Defines fields.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum Field {
		dpi, overwriteLines("overwrite-lines"), padding, shrinkPolygons("shrink-polygons");

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
	 * line.
	 * 
	 * @since 1.8
	 */
	public TesserocrSegmentLine() {
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
		return "ocrd-tesserocr-segment-line";
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
		return "ocr-d tesserocr segment line processor";
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
		ProcessorArgument argument = new ProcessorArgument();

		return new Model(
				new IntegerField(Field.dpi.getName(), argument.getDpi(), locale -> getString(locale, "dpi"),
						locale -> getString(locale, "dpi.description"), null, 1, -1, null, locale -> "pt", false),
				new BooleanField(Field.overwriteLines.getName(), argument.isOverwriteLines(),
						locale -> getString(locale, "overwrite.lines"),
						locale -> getString(locale, "overwrite.lines.description"), false),
				new IntegerField(Field.padding.getName(), argument.getPadding(), locale -> getString(locale, "padding"),
						locale -> getString(locale, "padding.description"), null, 1, 0, null, locale -> "px", false),
				new BooleanField(Field.shrinkPolygons.getName(), argument.isShrinkPolygons(),
						locale -> getString(locale, "shrink.polygons"),
						locale -> getString(locale, "shrink.polygons.description"), false));
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
				 * Overwrite lines parameter
				 */
				if (availableArguments.remove(Field.overwriteLines.getName()))
					try {
						final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
								Field.overwriteLines.getName());

						if (argument.getValue().isPresent())
							processorArgument.setOverwriteLines(argument.getValue().get());
					} catch (ClassCastException e) {
						updatedStandardError(
								"The argument '" + Field.overwriteLines.getName() + "' is not of boolean type.");

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
				 * Runs the processor
				 */
				return run(framework, processorArgument, availableArguments, () -> isCanceled,
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
		 * The pixel density.
		 */
		private int dpi = -1;

		/**
		 * True if overwrite lines.
		 */
		@JsonProperty("overwrite_lines")
		private boolean isOverwriteLines = true;

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

	}
}