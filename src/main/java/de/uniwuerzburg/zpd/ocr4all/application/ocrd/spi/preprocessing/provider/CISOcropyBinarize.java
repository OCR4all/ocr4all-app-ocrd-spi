/**
 * File:     CISOcropyBinarize.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.preprocessing.provider
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     29.03.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.preprocessing.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.spi.PreprocessingServiceProvider;
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
 * Defines service providers for ocr-d cis ocropy binarize. The following
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
public class CISOcropyBinarize extends OCRDServiceProviderWorker implements PreprocessingServiceProvider {
	/**
	 * The prefix of the message keys in the resource bundle.
	 */
	private static final String messageKeyPrefix = "preprocessing.cis.ocropy.binarize.";

	/**
	 * Defines fields.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum Field {
		method, threshold, grayscale, maxSkew("maximum-skewing"), noiseMaxSize("noise-maximum-size"),
		levelOperation("level-of-operation");

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
	 * Defines methods.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum Method {
		none, global, otsu, gaussOtsu("gauss-otsu"), ocropy;

		/**
		 * The default method.
		 */
		public static Method defaultMethod = ocropy;

		/**
		 * The value.
		 */
		private final String cisValue;

		/**
		 * Creates a method.
		 * 
		 * @since 1.8
		 */
		private Method() {
			cisValue = this.name();
		}

		/**
		 * Creates a method.
		 * 
		 * @param cisValue The cis value.
		 * @since 1.8
		 */
		private Method(String cisValue) {
			this.cisValue = cisValue;
		}

		/**
		 * Returns the cis value.
		 *
		 * @return The cis value.
		 * @since 1.8
		 */
		public String getCisValue() {
			return cisValue;
		}

		/**
		 * Returns the method with given name.
		 * 
		 * @param name The method name.
		 * @return The method with given name. Null if unknown.
		 * @since 1.8
		 */
		public static Method getMethod(String name) {
			if (name != null && !name.isBlank()) {
				name = name.trim();

				for (Method method : Method.values())
					if (method.name().equals(name))
						return method;
			}

			return null;
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
		page, region, line;

		/**
		 * The default method.
		 */
		public static LevelOperation defaultLevel = page;

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
	 * Default constructor for a service provider for ocr-d cis ocropy binarize.
	 * 
	 * @since 1.8
	 */
	public CISOcropyBinarize() {
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
		return "ocrd-cis-ocropy-binarize";
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
		return "ocr-d cis ocropy binarize processor";
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
		return Optional.of("fa-regular fa-image");
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
		ProcessorArgumentMethodOcropy argument = new ProcessorArgumentMethodOcropy();

		final List<SelectField.Item> methods = new ArrayList<SelectField.Item>();
		for (Method method : Method.values())
			methods.add(new SelectField.Option(method.name().equals(argument.getMethod()), method.name(),
					locale -> getString(locale, "method." + method.name())));

		final List<SelectField.Item> levelOperations = new ArrayList<SelectField.Item>();
		for (LevelOperation levelOperation : LevelOperation.values())
			levelOperations.add(
					new SelectField.Option(levelOperation.equals(argument.getLevelOperation()), levelOperation.name(),
							locale -> getMessage(locale, "page.xml.level.operation." + levelOperation.name())));

		return new Model(
				new SelectField(Field.method.getName(), locale -> getString(locale, "method"),
						locale -> getString(locale, "method.description"), false, methods, false),
				new IntegerField(Field.threshold.getName(), Math.round(argument.getThreshold() * 100),
						locale -> getString(locale, "threshold"), locale -> getString(locale, "threshold.description"),
						null, 1, 0, 100, locale -> "%", false),
				new BooleanField(Field.grayscale.getName(), argument.isGrayscale(),
						locale -> getString(locale, "grayscale"), locale -> getString(locale, "grayscale.description"),
						false),
				new DecimalField(Field.maxSkew.getName(), argument.getMaxSkew(),
						locale -> getString(locale, "maximum.skewing"),
						locale -> getString(locale, "maximum.skewing.description"), null, 0.1F, 0F, null, locale -> "°",
						false),
				new IntegerField(Field.noiseMaxSize.getName(), argument.getNoiseMaxSize(),
						locale -> getString(locale, "noise.maximum.size"),
						locale -> getString(locale, "noise.maximum.size.description"), null, 1, 0, null, locale -> "px",
						false),
				new SelectField(Field.levelOperation.getName(),
						locale -> getMessage(locale, "page.xml.level.operation"),
						locale -> getString(locale, "level.operation.description"), false, levelOperations, false));
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
				 * The arguments depends on method
				 */
				Method method = Method.defaultMethod;
				if (availableArguments.remove(Field.method.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.method.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								method = Method.getMethod(values.get(0));

								if (method == null) {
									updatedStandardError("Unknown method value"
											+ (values.get(0) == null || values.get(0).isBlank() ? ""
													: " '" + values.get(0).trim() + "'")

											+ ".");

									return ProcessServiceProvider.Processor.State.interrupted;
								}
							} else if (values.size() > 1) {
								updatedStandardError("Only one method can be selected.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
						}

					} catch (ClassCastException e) {
						updatedStandardError("The argument '" + Field.method.getName() + "' is not of selection type.");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

				/*
				 * Processor arguments
				 */
				ProcessorArgument processorArgument;

				switch (method) {
				case global:
					processorArgument = new ProcessorArgumentMethodOcropyGlobal();
					break;
				case ocropy:
					processorArgument = new ProcessorArgumentMethodOcropy();
					break;
				case none:
				case otsu:
				case gaussOtsu:
				default:
					processorArgument = new ProcessorArgument();
				}

				processorArgument.setMethod(method.getCisValue());

				/*
				 * Level of operation argument
				 */
				if (availableArguments.remove(Field.levelOperation.getName()))
					try {
						final SelectArgument argument = modelArgument.getArgument(SelectArgument.class,
								Field.levelOperation.getName());
						if (argument.getValues().isPresent()) {
							List<String> values = argument.getValues().get();

							if (values.size() == 1) {
								processorArgument.setLevelOperation(LevelOperation.getLevel(values.get(0)));

								if (processorArgument.getLevelOperation() == null) {
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

				if (processorArgument instanceof ProcessorArgumentMethodOcropyGlobal) {
					ProcessorArgumentMethodOcropyGlobal processorArgumentMethodOcropyGlobal = (ProcessorArgumentMethodOcropyGlobal) processorArgument;

					/*
					 * Threshold parameter
					 */
					if (availableArguments.remove(Field.threshold.getName()))
						try {
							final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
									Field.threshold.getName());

							if (argument.getValue().isPresent()) {
								int value = argument.getValue().get();

								if (value < 0 || value > 100) {
									updatedStandardError("The threshold value " + value + " is out of range [0..100].");

									return ProcessServiceProvider.Processor.State.interrupted;
								}

								processorArgumentMethodOcropyGlobal.setThreshold(value / 100F);
							}
						} catch (ClassCastException e) {
							updatedStandardError(
									"The argument '" + Field.threshold.getName() + "' is not of integer type.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

					if (processorArgument instanceof ProcessorArgumentMethodOcropy) {
						ProcessorArgumentMethodOcropy processorArgumentMethodOcropy = (ProcessorArgumentMethodOcropy) processorArgument;

						/*
						 * Grayscale parameter
						 */
						if (availableArguments.remove(Field.grayscale.getName()))
							try {
								final BooleanArgument argument = modelArgument.getArgument(BooleanArgument.class,
										Field.grayscale.getName());

								if (argument.getValue().isPresent())
									processorArgumentMethodOcropy.setGrayscale(argument.getValue().get());
							} catch (ClassCastException e) {
								updatedStandardError(
										"The argument '" + Field.grayscale.getName() + "' is not of boolean type.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}

						/*
						 * Maximum skewing parameter
						 */
						if (availableArguments.remove(Field.maxSkew.getName()))
							try {
								final DecimalArgument argument = modelArgument.getArgument(DecimalArgument.class,
										Field.maxSkew.getName());

								if (argument.getValue().isPresent())
									processorArgumentMethodOcropy.setMaxSkew(argument.getValue().get());
							} catch (ClassCastException e) {
								updatedStandardError(
										"The argument '" + Field.maxSkew.getName() + "' is not of decimal type.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}

						/*
						 * Noise maximum size parameter
						 */
						if (availableArguments.remove(Field.noiseMaxSize.getName()))
							try {
								final IntegerArgument argument = modelArgument.getArgument(IntegerArgument.class,
										Field.noiseMaxSize.getName());

								if (argument.getValue().isPresent())
									processorArgumentMethodOcropy.setNoiseMaxSize(argument.getValue().get());
							} catch (ClassCastException e) {
								updatedStandardError(
										"The argument '" + Field.noiseMaxSize.getName() + "' is not of integer type.");

								return ProcessServiceProvider.Processor.State.interrupted;
							}
					}
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
		 * The method.
		 */
		private String method = Method.defaultMethod.getCisValue();

		/**
		 * The level of operation.
		 */
		@JsonProperty("level-of-operation")
		private LevelOperation levelOperation = LevelOperation.defaultLevel;

		/**
		 * Returns the method.
		 *
		 * @return The method.
		 * @since 1.8
		 */
		public String getMethod() {
			return method;
		}

		/**
		 * Set the method.
		 *
		 * @param method The method to set.
		 * @since 1.8
		 */
		public void setMethod(String method) {
			this.method = method;
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
	}

	/**
	 * Defines processor arguments with default values for methods ocropy and
	 * global.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	public static class ProcessorArgumentMethodOcropyGlobal extends ProcessorArgument {
		/**
		 * The threshold.
		 */
		private float threshold = 0.5F;

		/**
		 * Returns the threshold.
		 *
		 * @return The threshold.
		 * @since 1.8
		 */
		public float getThreshold() {
			return threshold;
		}

		/**
		 * Set the threshold.
		 *
		 * @param threshold The threshold to set.
		 * @since 1.8
		 */
		public void setThreshold(float threshold) {
			this.threshold = threshold;
		}
	}

	/**
	 * Defines processor arguments with default values for methods ocropy.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	public static class ProcessorArgumentMethodOcropy extends ProcessorArgumentMethodOcropyGlobal {
		/**
		 * The grayscale.
		 */
		private boolean isGrayscale = false;

		/**
		 * The maximum skewing.
		 */
		@JsonProperty("maxskew")
		private float maxSkew = 0F;

		/**
		 * The noise maximum size.
		 */
		@JsonProperty("noise_maxsize")
		private int noiseMaxSize = 0;

		/**
		 * Returns the isGrayscale.
		 *
		 * @return The isGrayscale.
		 * @since 1.8
		 */
		public boolean isGrayscale() {
			return isGrayscale;
		}

		/**
		 * Set the isGrayscale.
		 *
		 * @param isGrayscale The isGrayscale to set.
		 * @since 1.8
		 */
		public void setGrayscale(boolean isGrayscale) {
			this.isGrayscale = isGrayscale;
		}

		/**
		 * Returns the maximum skewing.
		 *
		 * @return The maximum skewing.
		 * @since 1.8
		 */
		public float getMaxSkew() {
			return maxSkew;
		}

		/**
		 * Set the maximum skewing. If the skewing is not positive, it is set to 0.
		 *
		 * @param skewing The maximum skewing to set.
		 * @since 1.8
		 */
		public void setMaxSkew(float skewing) {
			if (skewing > 0)
				maxSkew = skewing;
			else
				maxSkew = 0;
		}

		/**
		 * Returns the noise maximum size.
		 *
		 * @return The noise maximum size.
		 * @since 1.8
		 */
		public int getNoiseMaxSize() {
			return noiseMaxSize;
		}

		/**
		 * Set the noise maximum size. If the noise is not positive, it is set to 0.
		 *
		 * @param noise The noise maximum size to set.
		 * @since 1.8
		 */
		public void setNoiseMaxSize(int noise) {
			if (noise > 0)
				noiseMaxSize = noise;
			else
				noiseMaxSize = 0;
		}
	}

}
