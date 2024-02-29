/**
 * File:     OCRDDockerJsonServiceProviderWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     08.07.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker;

import java.security.ProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Entry;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.Argument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;
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
public abstract class OCRDDockerJsonServiceProviderWorker extends OCRDDockerServiceProviderWorker
		implements ProcessServiceProvider {
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
	 * The service provider name.
	 */
	private final boolean isResources;

	/**
	 * The ProviderDescription.
	 */
	private ProviderDescription providerDescription;

	/**
	 * Creates an ocr-d service provider worker with JSON support and without
	 * resources.
	 * 
	 * @since 1.8
	 */
	public OCRDDockerJsonServiceProviderWorker() {
		this(false);
	}

	/**
	 * Creates an ocr-d service provider worker with JSON support.
	 * 
	 * @param isResources True if resources folder is required.
	 * @since 1.8
	 */
	public OCRDDockerJsonServiceProviderWorker(boolean isResources) {
		super(null);

		this.isResources = isResources;
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
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getName(java
	 * .util.Locale)
	 */
	@Override
	public String getName(Locale locale) {
		return getProcessorIdentifier();
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
			providerDescription = new ProviderDescription(process.getStandardOutput());
		else
			throw new ProviderException(
					process.getStandardError().trim() + " (process exit code " + process.getExitValue() + ")");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProviderCore#
	 * startCallback()
	 */
	@Override
	public void startCallback() throws ProviderException {
		if (providerDescription == null)
			initializeCallback();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProviderCore#
	 * restartCallback()
	 */
	@Override
	public void restartCallback() throws ProviderException {
		startCallback();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#
	 * getDescription(java.util.Locale)
	 */
	@Override
	public Optional<String> getDescription(Locale locale) {
		return providerDescription == null || !providerDescription.isDescriptionSet() ? super.getDescription(locale)
				: Optional.of(providerDescription.getDescription());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#
	 * getCategories()
	 */
	@Override
	public List<String> getCategories() {
		return providerDescription == null ? null : providerDescription.getCategories();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getSteps()
	 */
	@Override
	public List<String> getSteps() {
		return providerDescription == null ? null : providerDescription.getSteps();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getAdvice()
	 */
	@Override
	public String getAdvice() {
		return providerDescription == null || !providerDescription.isJsonSet() ? null
				: "JSON processor description:\n" + providerDescription.getJson();
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
				: new Premise(Premise.State.block, locale -> "The required 'docker' command is not available.");
	}

	/**
	 * Implementing subclasses that require entries at the beginning of the model
	 * can override this method to implement their logic. This method is called each
	 * time, when the model is required.
	 * 
	 * @param target    The target. Null if the model is generic, this means, it
	 *                  should not depend on a target.
	 * @param arguments The arguments in reading order.
	 * @return The entries to be added at the beginning of the model. Null or empty
	 *         if no entry is required.
	 * @since 1.8
	 */
	protected List<Entry> preModelEntries(Target target, List<String> arguments) {
		return null;
	}

	/**
	 * Implementing subclasses that require entries at the end of the model can
	 * override this method to implement their logic. This method is called each
	 * time, when the model is required.
	 * 
	 * @param target    The target. Null if the model is generic, this means, it
	 *                  should not depend on a target.
	 * @param arguments The arguments in reading order.
	 * @return The entries to be added at the end of the model. Null or empty if no
	 *         entry is required.
	 * @since 1.8
	 */
	protected List<Entry> posModelEntries(Target target, List<String> arguments) {
		return null;
	}

	/**
	 * Implementing subclasses that require special initialization on model fields
	 * can override this method to implement their logic. This method is called each
	 * time, when the model is required.
	 * 
	 * @param target    The target. Null if the model is generic, this means, it
	 *                  should not depend on a target.
	 * @param arguments The arguments in reading order.
	 * @return The model fields that need to be handled. The key is the field
	 *         argument and the value the desired field handler.
	 * @since 1.8
	 */
	protected Hashtable<String, ProviderDescription.ModelFactory.ModelFieldCallback> getModelCallbacks(Target target,
			List<String> arguments) {
		return null;
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
		return providerDescription.isModelFactorySet() ? providerDescription.getModelFactory().getModel(
				preModelEntries(target, providerDescription.getModelFactory().getArguments()),
				posModelEntries(target, providerDescription.getModelFactory().getArguments()),
				getModelCallbacks(target, providerDescription.getModelFactory().getArguments())) : null;
	}

	/**
	 * Implementing subclasses that require extra model arguments can override this
	 * method to implement their logic. This method is called each time, when a
	 * processor is executed.
	 * 
	 * @param processor The processor for service providers.
	 * @param arguments The arguments in reading order.
	 * @return The extra model arguments. Null or empty if no extra model argument
	 *         is required.
	 * @since 1.8
	 */
	protected List<Argument> extraArguments(CoreProcessorServiceProvider processor, List<String> arguments) {
		return null;
	}

	/**
	 * Implementing subclasses that require special initialization on model
	 * arguments can override this method to implement their logic. This method is
	 * called each time, when a processor is executed.
	 * 
	 * @param processor The processor for service providers.
	 * @param arguments The arguments in reading order.
	 * @return The model arguments that need to be handled. The key is the argument
	 *         name and the value the desired argument handler.
	 * @since 1.8
	 */
	protected Hashtable<String, ProviderDescription.ModelFactory.ModelArgumentCallback> getProcessorCallbacks(
			CoreProcessorServiceProvider processor, List<String> arguments) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider#
	 * newProcessor()
	 */
	@Override
	public Processor newProcessor() {
		return providerDescription == null || !providerDescription.isModelFactorySet() ? null
				: new OCRDDockerProcessorServiceProvider() {
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

						ObjectNode arguments;
						try {
							/*
							 * Parse the processor arguments
							 */
							updatedStandardOutput("Parse parameters.");

							final List<String> modelAarguments = providerDescription.getModelFactory().getArguments();
							arguments = providerDescription.getModelFactory().getJson(modelArgument,
									getProcessorCallbacks(this, modelAarguments),
									extraArguments(this, modelAarguments));
						} catch (Exception e) {
							updatedStandardError(e.getMessage());

							return ProcessServiceProvider.Processor.State.interrupted;
						}

						/*
						 * Runs the processor
						 */
						return run(framework, isResources, arguments, null, dockerProcess, () -> isCanceled(),
								() -> complete(), message -> updatedStandardOutput(message),
								message -> updatedStandardError(message),
								progress -> callback.updatedProgress(progress), 0.01F);

					}
				};
	}

}
