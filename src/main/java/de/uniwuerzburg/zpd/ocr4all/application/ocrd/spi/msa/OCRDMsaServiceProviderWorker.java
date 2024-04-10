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
import de.uniwuerzburg.zpd.ocr4all.application.communication.msa.api.domain.JobResponse;
import de.uniwuerzburg.zpd.ocr4all.application.communication.msa.api.domain.SystemJobResponse;
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
 * <li>msa-timeout-active-processor: 15000</li>
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
	 * The job request mapping.
	 */
	public static final String jobRequestMapping = schedulerControllerContextPath + "job/{id}";

	/**
	 * The system job request mapping.
	 */
	public static final String systemJobRequestMapping = processorControllerContextPath + "job/{id}";

	/**
	 * The expunge job request mapping.
	 */
	public static final String expungeJobRequestMapping = schedulerControllerContextPath + "expunge/{id}";

	/**
	 * The processor json description request mapping.
	 */
	private static final String jsonDescriptionRequestMapping = processorControllerContextPath
			+ "description/json/{processor}";

	/**
	 * The processor json description request mapping.
	 */
	private static final String executeRequestMapping = processorControllerContextPath + "execute";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		hostId("msa-host-id", "ocrd"), applicationLayerProtocol("msa-host-protocol", "http"),
		timeoutActiveProcessor("msa-timeout-active-processor", "15000");

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
	 * The logger.
	 */
	protected final org.slf4j.Logger logger;

	/**
	 * The ProviderDescription.
	 */
	private ProviderDescription providerDescription = null;

	/**
	 * The client to perform HTTP requests.
	 */
	protected RestClient restClient = null;

	/**
	 * The timeout for the active processor.
	 */
	protected final long timeoutActiveProcessor;

	/**
	 * Default constructor for an ocr-d microservice architecture (MSA) service
	 * provider worker.
	 * 
	 * @param logger The logger class.
	 * @since 17
	 */
	public OCRDMsaServiceProviderWorker(Class<?> logger) {
		super();

		this.logger = org.slf4j.LoggerFactory.getLogger(logger);

		long timeoutActiveProcessor;
		try {
			timeoutActiveProcessor = Long
					.parseLong(configuration.getValue(ServiceProviderCollection.timeoutActiveProcessor));
		} catch (Exception e) {
			timeoutActiveProcessor = Long.parseLong(ServiceProviderCollection.timeoutActiveProcessor.getDefaultValue());
		}

		this.timeoutActiveProcessor = timeoutActiveProcessor > 0 ? timeoutActiveProcessor : 0;
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

		restClient = RestClient.create(
				configuration.getValue(ServiceProviderCollection.applicationLayerProtocol) + "://" + host.getUrl());

		try {
			providerDescription = new ProviderDescription(restClient.get()
					.uri(jsonDescriptionRequestMapping, getProcessorIdentifier()).accept(MediaType.APPLICATION_JSON)
					.retrieve().onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
						throw new ProviderException("HTTP client error status " + response.getStatusCode() + " ("
								+ response.getStatusText() + "): " + response.getHeaders());
					}).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
						throw new ProviderException("HTTP server error status " + response.getStatusCode() + " ("
								+ response.getStatusText() + "): " + response.getHeaders());
					}).body(DescriptionResponse.class).getDescription());
		} catch (Exception e) {
			logger.warn("provider " + getProcessorIdentifier() + " could not be initialized - " + e.getMessage());

			throw e;
		}
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
			final String message = "trouble contacting ocrd msa - " + e.getMessage();

			logger.warn(getProcessorIdentifier() + ": " + message);

			return new Premise(Premise.State.block, locale -> message);
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
					/**
					 * The timeout thread.
					 */
					private Thread thread = null;

					/**
					 * Logs the trouble.
					 * 
					 * @param message The trouble message.
					 * @since 17
					 */
					private void logTrouble(String message) {
						logger.warn(getProcessorIdentifier() + ": " + message);
						updatedStandardError(message);
					}

					/**
					 * Maps the msa job state to the execution process state and returns it. The msa
					 * job has to be done.
					 * 
					 * @param state The msa job state.
					 * @return The state of the execution of the process.
					 * @since 17
					 */
					private State map(de.uniwuerzburg.zpd.ocr4all.application.communication.msa.job.State state) {
						switch (state) {
						case canceled:
							return ProcessServiceProvider.Processor.State.canceled;
						case completed:
							return ProcessServiceProvider.Processor.State.completed;
						case interrupted:
						default:
							return ProcessServiceProvider.Processor.State.interrupted;
						}

					}

					/*
					 * (non-Javadoc)
					 * 
					 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.msa.
					 * OCRDMsaProcessorServiceProvider#handle(de.uniwuerzburg.zpd.ocr4all.
					 * application.communication.message.spi.EventSPI)
					 */
					@Override
					protected void handle(EventSPI event) {
						final String message = "received event " + event.getType().name() + " (" + event.getCreatedAt()
								+ ") - " + event.getMessage();

						logger.debug(getProcessorIdentifier() + ": " + message);

						if (event.getType().equals(EventSPI.Type.interrupted))
							updatedStandardError(message);
						else
							updatedStandardOutput(message);

						if (thread != null && thread.isAlive() && !thread.isInterrupted())
							thread.interrupt();
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
							updatedStandardError("undefined framework.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

						try {
							ping();
						} catch (ProviderException e) {
							logTrouble("trouble contacting ocrd msa - " + e.getMessage());

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

						final Path pathProcessor = framework.getProcessorWorkspaceRelativeProjects();
						if (pathProcessor == null) {
							logTrouble("invalid working directory '" + framework.getProcessorWorkspace().toString()
									+ "'.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}

						/*
						 * Runs the processor
						 */
						return run(framework, arguments, () -> isCanceled(), () -> complete(),
								message -> updatedStandardOutput(message), message -> updatedStandardError(message),
								progress -> callback.updatedProgress(progress), 0.01F,
								(metsFileGroup, argumentsJsonSerialization) -> {
									// register event handler
									registerEventHandler();

									logger.debug(
											getProcessorIdentifier() + ": process request - key " + key + ", home '"
													+ pathProcessor.toString(),
											"', input '" + metsFileGroup.getInput() + "', output '"
													+ metsFileGroup.getOutput() + "', arguments '"
													+ argumentsJsonSerialization + "'.");

									final ProcessRequest processRequest = new ProcessRequest(key,
											getProcessorIdentifier(), pathProcessor.toString(),
											metsFileGroup.getInput(), metsFileGroup.getOutput(),
											Arrays.asList("-p", argumentsJsonSerialization));

									// start the job
									JobResponse jobResponse;
									try {
										jobResponse = restClient.post().uri(executeRequestMapping)
												.contentType(MediaType.APPLICATION_JSON).body(processRequest)
												.accept(MediaType.APPLICATION_JSON).retrieve()
												.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
													throw new ProviderException("HTTP client error status "
															+ response.getStatusCode() + " (" + response.getStatusText()
															+ "): " + response.getHeaders());
												}).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
													throw new ProviderException("HTTP server error status "
															+ response.getStatusCode() + " (" + response.getStatusText()
															+ "): " + response.getHeaders());
												}).body(JobResponse.class);
									} catch (Exception e) {
										logTrouble("could not execute processor, key " + key + " - '" + e.getMessage());

										return ProcessServiceProvider.Processor.State.interrupted;
									}

									final int jobId = jobResponse.getId();

									logger.debug(
											getProcessorIdentifier() + ": running job " + jobId + ", key " + key + ".");

									// wait until the job is done
									while (!jobResponse.getState().isDone()) {
										thread = new Thread(() -> {
											try {
												logger.debug("thread wait: job " + jobId + ", key " + key + ".");

												Thread.sleep(timeoutActiveProcessor);

												logger.debug("thread timeout: job " + jobId + ", key " + key + ".");
											} catch (InterruptedException e) {
												logger.debug("thread interrupted by event: job " + jobId + ", key "
														+ key + ".");
											}
										});

										thread.start();

										// Wait for a timeout or a new event
										try {
											thread.join();
										} catch (InterruptedException e) {
											// Nothing to do
										}

										// restore the current job status
										try {
											jobResponse = restClient.get().uri(jobRequestMapping, jobId)
													.accept(MediaType.APPLICATION_JSON).retrieve()
													.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
														throw new ProviderException(
																"HTTP client error status " + response.getStatusCode()
																		+ " (" + response.getStatusText() + "): "
																		+ response.getHeaders());
													})
													.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
														throw new ProviderException(
																"HTTP server error status " + response.getStatusCode()
																		+ " (" + response.getStatusText() + "): "
																		+ response.getHeaders());
													}).body(JobResponse.class);
										} catch (Exception e) {
											logTrouble("could not restore the job " + jobId + ", key " + key + " - "
													+ e.getMessage());

											return ProcessServiceProvider.Processor.State.interrupted;
										}
									}

									// job is done, unregister event handler
									unregisterEventHandler();

									// restore the system job information
									try {
										SystemJobResponse systemJobResponse = restClient.get()
												.uri(systemJobRequestMapping, jobId).accept(MediaType.APPLICATION_JSON)
												.retrieve()
												.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
													throw new ProviderException("HTTP client error status "
															+ response.getStatusCode() + " (" + response.getStatusText()
															+ "): " + response.getHeaders());
												}).onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
													throw new ProviderException("HTTP server error status "
															+ response.getStatusCode() + " (" + response.getStatusText()
															+ "): " + response.getHeaders());
												}).body(SystemJobResponse.class);

										if (systemJobResponse.getStandardOutput() != null
												&& !systemJobResponse.getStandardOutput().isBlank())
											updatedStandardOutput(systemJobResponse.getStandardOutput());

										if (systemJobResponse.getStandardError() != null
												&& !systemJobResponse.getStandardError().isBlank())
											updatedStandardError(systemJobResponse.getStandardError());

										if (systemJobResponse.getExitValue() > 0)
											updatedStandardError(
													"processor exit code: " + systemJobResponse.getExitValue());

										try {
											restClient.get().uri(expungeJobRequestMapping, jobId).retrieve()
													.onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
														throw new ProviderException(
																"HTTP client error status " + response.getStatusCode()
																		+ " (" + response.getStatusText() + "): "
																		+ response.getHeaders());
													})
													.onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
														throw new ProviderException(
																"HTTP server error status " + response.getStatusCode()
																		+ " (" + response.getStatusText() + "): "
																		+ response.getHeaders());
													}).toBodilessEntity();
										} catch (Exception e) {
											logTrouble("could not expunge the job " + jobId + ", key " + key + " - "
													+ e.getMessage());
										}

										return map(systemJobResponse.getState());
									} catch (Exception e) {
										logTrouble("could not restore system information of the job " + jobId + ", key "
												+ key + " - " + e.getMessage());

										return map(jobResponse.getState());
									}
								});
					}
				};
	}
}
