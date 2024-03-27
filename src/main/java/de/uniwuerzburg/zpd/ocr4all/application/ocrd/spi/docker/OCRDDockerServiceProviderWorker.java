/**
 * File:     OCRDDockerServiceProviderWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     29.03.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core.OCRDServiceProviderWorker;
import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.OCRDUtils;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.util.MetsUtils;
import de.uniwuerzburg.zpd.ocr4all.application.spi.util.SystemProcess;

/**
 * Defines ocr-d docker service provider workers. The following properties of
 * the service provider collection <b>ocr-d</b> override the local default
 * settings (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>uid: &lt;effective system user ID. -1 if not defined&gt;</li>
 * <li>gid: &lt;effective system group ID. -1 if not defined&gt;</li>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * <li>docker-image: ocrd/all:2023-04-02</li>
 * <li>docker-resources: /usr/local/share/ocrd-resources</li>
 * <li>docker-stop-wait-kill-seconds: 2</li>
 * <li>see {@link OCRDServiceProviderWorker} for remainder settings</li>
 * </ul>
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 1.8
 */
public abstract class OCRDDockerServiceProviderWorker extends OCRDServiceProviderWorker {
	/**
	 * The base name of the resource bundle, a fully qualified class name.
	 */
	private static final String resourceBundleBaseName = "messages";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		uid("uid", null), gid("gid", null), dockerImage("docker-image", "ocrd/all:2023-04-02"),
		dockerResources("docker-resources", "/usr/local/share/ocrd-resources"),
		dockerStopWaitKillSeconds("docker-stop-wait-kill-seconds", "2");

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
	 * The prefix of the message keys in the resource bundle.
	 */
	protected final String resourceBundleKeyPrefix;

	/**
	 * Default constructor for an ocr-d docker service provider worker.
	 * 
	 * @since 1.8
	 */
	public OCRDDockerServiceProviderWorker() {
		this(null);
	}

	/**
	 * Creates an ocr-d service provider worker.
	 * 
	 * @param resourceBundleKeyPrefix The prefix of the keys in the resource bundle.
	 * @since 1.8
	 */
	public OCRDDockerServiceProviderWorker(String resourceBundleKeyPrefix) {
		super();

		this.resourceBundleKeyPrefix = resourceBundleKeyPrefix == null ? "" : resourceBundleKeyPrefix.trim();
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
		return super.getProvider() + "/docker";
	}

	/**
	 * Returns the resource bundle using the specified locale.
	 * 
	 * @param locale The locale for which a resource bundle is desired.
	 * @return The resource bundle.
	 * @since 1.8
	 */
	private static ResourceBundle getResourceBundle(Locale locale) {
		return ResourceBundle.getBundle(resourceBundleBaseName, locale,
				OCRDDockerServiceProviderWorker.class.getClassLoader());
	}

	/**
	 * Returns the string for the given key from the resource bundle. Returns the
	 * key with "?" at the beginning and at the end, if the resource is missed.
	 * 
	 * @param locale The locale for which a resource bundle is desired.
	 * @param key    The message key for the desired string.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getString(Locale locale, String key) {
		return getString(locale, key, null, "?" + resourceBundleKeyPrefix + key + "?");
	}

	/**
	 * Returns the string for the given key from the resource bundle. Returns the
	 * key with "?" at the beginning and at the end, if the resource is missed.
	 * 
	 * @param locale The locale for which a resource bundle is desired.
	 * @param key    The message key for the desired string.
	 * @param args   An array of arguments that will be filled in for params within
	 *               the message (params look like "{0}", "{1,date}", "{2,time}"
	 *               within a message), or null if none.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getString(Locale locale, String key, Object[] args) {
		return getString(locale, key, args, "?" + resourceBundleKeyPrefix + key + "?");
	}

	/**
	 * Returns the string for the given key from the resource bundle.
	 * 
	 * @param locale        The locale for which a resource bundle is desired.
	 * @param key           The message key for the desired string.
	 * @param defaultString The default string if the resource is missed.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getString(Locale locale, String key, String defaultString) {
		return getString(locale, key, null, defaultString);
	}

	/**
	 * Returns the string for the given key from the resource bundle.
	 * 
	 * @param locale        The locale for which a resource bundle is desired.
	 * @param key           The message key for the desired string.
	 * @param args          An array of arguments that will be filled in for params
	 *                      within the message (params look like "{0}", "{1,date}",
	 *                      "{2,time}" within a message), or null if none.
	 * @param defaultString The default string if the resource is missed.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getString(Locale locale, String key, Object[] args, String defaultString) {
		try {
			String pattern = getResourceBundle(locale)
					.getString(resourceBundleKeyPrefix + (key == null ? "" : key.trim()));

			return args == null ? pattern : MessageFormat.format(pattern, args);
		} catch (MissingResourceException e) {
			return defaultString;
		}
	}

	/**
	 * Returns the message for the given key from the resource bundle. No prefix is
	 * added to the message keys.
	 * 
	 * @param locale The locale for which a resource bundle is desired.
	 * @param key    The message key for the desired string.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getMessage(Locale locale, String key) {
		return getMessage(locale, key, null);
	}

	/**
	 * Returns the message for the given key from the resource bundle. No prefix is
	 * added to the message keys.
	 * 
	 * @param locale The locale for which a resource bundle is desired.
	 * @param key    The message key for the desired string.
	 * @param args   An array of arguments that will be filled in for params within
	 *               the message (params look like "{0}", "{1,date}", "{2,time}"
	 *               within a message), or null if none.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getMessage(Locale locale, String key, Object[] args) {
		return getMessage(locale, key, args, "?" + key + "?");
	}

	/**
	 * Returns the message for the given key from the resource bundle. No prefix is
	 * added to the message keys.
	 * 
	 * @param locale        The locale for which a resource bundle is desired.
	 * @param key           The message key for the desired string.
	 * @param args          An array of arguments that will be filled in for params
	 *                      within the message (params look like "{0}", "{1,date}",
	 *                      "{2,time}" within a message), or null if none.
	 * @param defaultString The default string if the resource is missed.
	 * @return the string for the given key.
	 * @since 1.8
	 */
	protected String getMessage(Locale locale, String key, Object[] args, String defaultString) {
		try {
			String pattern = getResourceBundle(locale).getString((key == null ? "" : key.trim()));

			return args == null ? pattern : MessageFormat.format(pattern, args);
		} catch (MissingResourceException e) {
			return defaultString;
		}
	}

	/**
	 * Returns the docker process with working directory of the current Java
	 * process.
	 * 
	 * @return The docker process with working directory of the current Java
	 *         process.
	 * @since 1.8
	 */
	protected SystemProcess getDockerProcess() {
		return getDockerProcess(null);
	}

	/**
	 * Returns the docker process.
	 * 
	 * @param framework The framework. If null, uses the working directory of the
	 *                  current Java process.
	 * @return The docker process.
	 * @since 1.8
	 */
	protected SystemProcess getDockerProcess(Framework framework) {
		String dockerCommand = configuration.getSystemCommand(SystemCommand.Type.docker).getCommand().toString();

		return new SystemProcess(framework == null ? null : framework.getProcessorWorkspace(), dockerCommand);
	}

	/**
	 * Returns the docker image.
	 * 
	 * @return The docker image.
	 * @since 1.8
	 */
	protected String getDockerImage() {
		return configuration.getValue(ServiceProviderCollection.dockerImage);
	}

	/**
	 * Returns the ocr-d arguments for the docker process.
	 * 
	 * @param framework     The framework.
	 * @param isResources   True if resources folder is required.
	 * @param dockerName    The docker name.
	 * @param arguments     The ocr-d processor arguments.
	 * @param metsFileGroup The mets file group.
	 * @return The ocr-d arguments for the docker process.
	 * @throws JsonProcessingException Throws on processing (parsing, generating)
	 *                                 JSON arguments that are not pure I/O
	 *                                 problems.
	 * @since 1.8
	 */
	protected List<String> getProcessorArguments(Framework framework, boolean isResources, String dockerName,
			Object arguments, MetsUtils.FrameworkFileGroup metsFileGroup) throws JsonProcessingException {
		// Get the effective system user/group id
		String uid = configuration.getValue(ServiceProviderCollection.uid);
		if (uid == null && framework.isUID())
			uid = "" + framework.getUID();

		if (uid != null) {
			String gid = configuration.getValue(ServiceProviderCollection.gid);
			if (gid == null && framework.isGID())
				gid = "" + framework.getGID();

			if (gid != null)
				uid += ":" + gid;
		}

		// Build the processor arguments
		List<String> processorArguments = new ArrayList<>(Arrays.asList("run", "--rm", "--name", dockerName));

		if (uid != null)
			processorArguments.addAll(Arrays.asList("-u", uid));

		if (isResources) {
			Path optResources = getOptResources(framework);
			Path dockerResources = getDockerResources();

			if (Files.isDirectory(optResources))
				processorArguments
						.addAll(Arrays.asList("-v", optResources.toString() + ":" + dockerResources.toString()));
		}

		processorArguments.addAll(Arrays.asList("-v", framework.getProcessorWorkspace().toString() + ":/data", "-w",
				"/data", "--", getDockerImage(), getProcessorIdentifier(), "-I", metsFileGroup.getInput(), "-O",
				metsFileGroup.getOutput()));

		if (arguments != null)
			processorArguments.addAll(Arrays.asList("-p", objectMapper.writeValueAsString(arguments)));

		return processorArguments;
	}

	/**
	 * Runs the ocr-d processor without resources folder.
	 * 
	 * @param framework            The framework.
	 * @param arguments            The processor arguments.
	 * @param unnecessaryArguments The unnecessary processor arguments. Null or
	 *                             empty means that there are no unnecessary
	 *                             processor arguments.
	 * @param dockerProcess        The docker process.
	 * @param runningState         The callback for processor running state.
	 * @param execution            The callback for processor execution.
	 * @param standardOutput       The callback for standard output.
	 * @param standardError        The callback for standard error.
	 * @param progress             The callback for progress.
	 * @param baseProgress         The base progress.
	 * @return The processor execution state.
	 * @since 1.8
	 */
	protected ProcessServiceProvider.Processor.State run(Framework framework, Object arguments,
			Set<String> unnecessaryArguments, OCRDDockerProcessorServiceProvider.DockerProcess dockerProcess,
			ProcessorRunningState runningState, ProcessorExecution execution, Message standardOutput,
			Message standardError, Progress progress, float baseProgress) {

		return run(framework, false, arguments, unnecessaryArguments, dockerProcess, runningState, execution,
				standardOutput, standardError, progress, baseProgress);
	}

	/**
	 * Runs the ocr-d processor.
	 * 
	 * @param framework            The framework.
	 * @param isResources          True if resources folder is required.
	 * @param arguments            The processor arguments.
	 * @param unnecessaryArguments The unnecessary processor arguments. Null or
	 *                             empty means that there are no unnecessary
	 *                             processor arguments.
	 * @param dockerProcess        The docker process.
	 * @param runningState         The callback for processor running state.
	 * @param execution            The callback for processor execution.
	 * @param standardOutput       The callback for standard output.
	 * @param standardError        The callback for standard error.
	 * @param progress             The callback for progress.
	 * @param baseProgress         The base progress.
	 * @return The processor execution state.
	 * @since 1.8
	 */
	protected ProcessServiceProvider.Processor.State run(Framework framework, boolean isResources, Object arguments,
			Set<String> unnecessaryArguments, OCRDDockerProcessorServiceProvider.DockerProcess dockerProcess,
			ProcessorRunningState runningState, ProcessorExecution execution, Message standardOutput,
			Message standardError, Progress progress, float baseProgress) {
		if (unnecessaryArguments != null && !unnecessaryArguments.isEmpty())
			standardOutput.update("Ignored unnecessary parameters: " + unnecessaryArguments + ".");

		return run(framework, arguments, runningState, execution, standardOutput, standardError, progress, baseProgress,
				(metsFileGroup, argumentsJsonSerialization) -> {
					String dockerName = "ocr4all-" + OCRDUtils.getUUID();

					List<String> processorArguments;

					try {
						processorArguments = getProcessorArguments(framework, isResources, dockerName, arguments,
								metsFileGroup);
					} catch (IOException e) {
						standardError
								.update("troubles running " + getProcessorDescription() + " - " + e.getMessage() + ".");

						return ProcessServiceProvider.Processor.State.interrupted;
					}

					dockerProcess
							.configure(
									getDockerProcess(framework), getDockerProcess(), Arrays
											.asList("stop",
													"--time=" + ConfigurationServiceProvider.getValue(configuration,
															ServiceProviderCollection.dockerStopWaitKillSeconds),
													dockerName));

					standardOutput.update("Execute docker process '" + dockerProcess.getProcess().getCommand()
							+ "' with parameters: " + processorArguments + ".");

					ProcessServiceProvider.Processor.State state = null;

					try {
						dockerProcess.getProcess().execute(processorArguments);

						updateProcessorMessages(dockerProcess.getProcess(), standardOutput, standardError);

						if (runningState.isCanceled())
							state = ProcessServiceProvider.Processor.State.canceled;
						else if (dockerProcess.getProcess().getExitValue() != 0) {
							standardError.update("Cannot run " + getProcessorDescription() + ", exit code "
									+ dockerProcess.getProcess().getExitValue() + ".");

							state = ProcessServiceProvider.Processor.State.interrupted;
						}
					} catch (IOException e) {
						updateProcessorMessages(dockerProcess.getProcess(), standardOutput, standardError);

						standardError
								.update("troubles running " + getProcessorDescription() + " - " + e.getMessage() + ".");

						state = ProcessServiceProvider.Processor.State.interrupted;
					}

					return state;
				});
	}

	/**
	 * Returns the docker resources folder.
	 * 
	 * @return The docker resources folder.
	 * @since 1.8
	 */
	protected Path getDockerResources() {
		return getDockerResources(configuration);
	}

	/**
	 * Returns the docker resources folder.
	 * 
	 * @param configuration The service provider configuration.
	 * @return The docker resources folder.
	 * @since 1.8
	 */
	protected Path getDockerResources(ConfigurationServiceProvider configuration) {
		return Paths
				.get(ConfigurationServiceProvider.getValue(configuration, ServiceProviderCollection.dockerResources),
						ConfigurationServiceProvider.getValue(configuration, processorIdentifier()))
				.normalize();
	}

}
