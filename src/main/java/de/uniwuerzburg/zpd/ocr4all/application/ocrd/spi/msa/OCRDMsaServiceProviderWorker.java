/**
 * File:     OCRDMsaServiceProviderWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     01.03.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa;

import java.nio.file.Path;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.uniwuerzburg.zpd.ocr4all.application.communication.message.spi.EventSPI;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.communication.api.DescriptionResponse;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.communication.api.ProcessRequest;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.MicroserviceArchitecture;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Entry;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.Argument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;

/**
 * Defines ocr-d microservice architecture (MSA) service provider workers. The
 * following properties of the service provider collection <b>ocr-d</b> override
 * the local default settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>msa-host-id: ocrd</li>
 * <li>msa-host-protocol: http</li>
 * <li>see {@link OCRDServiceProviderWorker} for remainder settings</li>
 * </ul>
 *
 * 
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public abstract class OCRDMsaServiceProviderWorker extends OCRDServiceProviderWorker implements ProcessServiceProvider {
	/**
	 * The api context path.
	 */
	public static final String apiContextPath = "/api";

	/**
	 * The api version 1.0 prefix path.
	 */
	public static final String apiContextPathVersion_1_0 = apiContextPath + "/v1.0/";

	/**
	 * The scheduler controller context path.
	 */
	private static final String schedulerControllerContextPath = apiContextPathVersion_1_0 + "scheduler/";

	/**
	 * The processor controller context path.
	 */
	private static final String processorControllerContextPath = apiContextPathVersion_1_0 + "processor/";

	/**
	 * The ping request mapping.
	 */
	public static final String pingRequestMapping = schedulerControllerContextPath + "ping";

	/**
	 * The processor json description request mapping.
	 */
	private static final String jsonDescriptionRequestMapping = processorControllerContextPath
			+ "description/json/{processor}";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		hostId("msa-host-id", "ocrd"), applicationLayerProtocol("msa-host-protocol", "http");

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
	 * The ProviderDescription.
	 */
	private ProviderDescription providerDescription = null;

	/**
	 * The client to perform HTTP requests.
	 */
	protected RestClient restClient = null;

	/**
	 * Default constructor for an ocr-d microservice architecture (MSA) service
	 * provider worker.
	 * 
	 * @since 17
	 */
	public OCRDMsaServiceProviderWorker() {
		super();
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
		return super.getProvider() + "/msa";
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
	protected void initializeCallback() throws ProviderException {
		final String hostId = configuration.getValue(ServiceProviderCollection.hostId);

		MicroserviceArchitecture.Host host = microserviceArchitecture.getHost(hostId);

		if (host == null)
			throw new ProviderException("unknown host configuration for msa id " + hostId + ".");

		restClient = RestClient.builder().baseUrl(
				configuration.getValue(ServiceProviderCollection.applicationLayerProtocol) + "://" + host.getUrl())
				.build();

		providerDescription = new ProviderDescription(restClient.get()
				.uri(jsonDescriptionRequestMapping, getProcessorIdentifier()).accept(MediaType.APPLICATION_JSON)
				.retrieve().onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
					throw new ProviderException("HTTP client error status " + response.getStatusCode() + " ("
							+ response.getStatusText() + "): " + response.getHeaders());
				}).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
					throw new ProviderException("HTTP server error status " + response.getStatusCode() + " ("
							+ response.getStatusText() + "): " + response.getHeaders());
				}).body(DescriptionResponse.class).getDescription());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProviderCore#
	 * startCallback()
	 */
	@Override
	protected void startCallback() throws ProviderException {
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

	/**
	 * Ping the client to check the status of an HTTP request.
	 * 
	 * @throws ProviderException Throws on HTTP request troubles.
	 * @since 17
	 */
	private void ping() throws ProviderException {
		restClient.get().uri(pingRequestMapping).retrieve()
				.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
					throw new ProviderException("HTTP client error status " + response.getStatusCode() + " ("
							+ response.getStatusText() + "): " + response.getHeaders());
				}).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
					throw new ProviderException("HTTP server error status " + response.getStatusCode() + " ("
							+ response.getStatusText() + "): " + response.getHeaders());
				}).toBodilessEntity();
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
		try {
			ping();

			return new Premise();
		} catch (ProviderException e) {
			return new Premise(Premise.State.block, locale -> e.getMessage());
		}
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
				: new OCRDMsaProcessorServiceProvider(microserviceArchitecture.getEventController()) {
					/*
					 * (non-Javadoc)
					 * 
					 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.
					 * OCRDMsaProcessorServiceProvider#handle(de.uniwuerzburg.zpd.ocr4all.
					 * application.communication.message.spi.EventSPI)
					 */
					@Override
					protected void handle(EventSPI event) {
						// TODO Auto-generated method stub

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
						if (framework == null) {
							updatedStandardError("no framework is defined for the processor.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

						try {
							ping();
						} catch (ProviderException e) {
							updatedStandardError(e.getMessage());

							return ProcessServiceProvider.Processor.State.interrupted;
						}

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

						Path pathProject = framework.getProjects();
						Path pathWorkspace = framework.getProcessorWorkspace();
						if (!pathWorkspace.startsWith(pathProject) || pathWorkspace.equals(pathProject)) {
							updatedStandardError("invalid working directory '" + pathWorkspace.toString()
									+ "' for the processor, since it does not includes project directory '"
									+ pathProject.toString() + "'.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

						/*
						 * Runs the processor
						 */
						return run(framework, arguments, () -> isCanceled(), () -> complete(),
								message -> updatedStandardOutput(message), message -> updatedStandardError(message),
								progress -> callback.updatedProgress(progress), 0.01F,
								(metsFileGroup, argumentsJsonSerialization) -> {
									registerEventHandler();

									// TODO: call rest API and handle events -> method void handle(EventSPI event)
									ProcessServiceProvider.Processor.State state = null;

									ProcessRequest processRequest = new ProcessRequest(key, getProcessorIdentifier(),
											pathWorkspace.toString().substring(pathProject.toString().length() + 1),
											metsFileGroup.getInput(), metsFileGroup.getOutput(),
											Arrays.asList("-p", argumentsJsonSerialization));

									unregisterEventHandler();

									return state;
								});

					}
				};
	}
}
