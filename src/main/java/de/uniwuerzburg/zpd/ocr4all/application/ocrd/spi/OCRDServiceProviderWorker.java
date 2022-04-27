/**
 * File:     OCRDServiceProviderWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     29.03.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.util.SystemProcess;

/**
 * Defines ocr-d service provider workers. The following properties of the
 * service provider collection <b>ocr-d</b> override the local default settings
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
public abstract class OCRDServiceProviderWorker {
	/**
	 * The base name of the resource bundle, a fully qualified class name.
	 */
	private static final String resourceBundleBaseName = "messages";

	/**
	 * The collection name.
	 */
	protected static final String collectionName = "ocr-d";

	/**
	 * The JSON object mapper.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();
	{
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements Framework.ServiceProviderCollectionKey {
		uid("uid", null), gid("gid", null), optFolder("opt-folder", "ocr-d"),
		optResources("opt-resources", "resources"), dockerImage("docker-image", "ocrd/all:maximum"),
		dockerResources("docker-resources", "/usr/local/share/ocrd-resources");

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
	 * Creates an ocr-d service provider worker.
	 * 
	 * @since 1.8
	 */
	public OCRDServiceProviderWorker() {
		this(null);
	}

	/**
	 * Creates an ocr-d service provider worker.
	 * 
	 * @param resourceBundleKeyPrefix The prefix of the keys in the resource bundle.
	 * @since 1.8
	 */
	public OCRDServiceProviderWorker(String resourceBundleKeyPrefix) {
		super();

		this.resourceBundleKeyPrefix = resourceBundleKeyPrefix == null ? "" : resourceBundleKeyPrefix.trim();
	}

	/**
	 * Returns the processor name.
	 * 
	 * @return The processor name.
	 * @since 1.8
	 */
	protected abstract String processorName();

	/**
	 * Returns the processor description.
	 * 
	 * @return The processor description.
	 * @since 1.8
	 */
	protected abstract String processorDescription();

	/**
	 * Returns the resource bundle using the specified locale.
	 * 
	 * @param locale The locale for which a resource bundle is desired.
	 * @return The resource bundle.
	 * @since 1.8
	 */
	private static ResourceBundle getResourceBundle(Locale locale) {
		return ResourceBundle.getBundle(resourceBundleBaseName, locale,
				OCRDServiceProviderWorker.class.getClassLoader());
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
	 * Returns the docker process.
	 * 
	 * @param framework The framework.
	 * @return The docker process.
	 * @since 1.8
	 */
	protected SystemProcess getDockerProcess(Framework framework) {
		String dockerCommand = framework.getConfiguration().getSystemCommand(SystemCommand.Type.docker).getCommand()
				.toString();

		return new SystemProcess(framework.getProcessorWorkspace(), dockerCommand);
	}

	/**
	 * Returns the ocr-d arguments for the docker process.
	 * 
	 * @param framework   The framework.
	 * @param isResources True if resources folder is required.
	 * @param arguments   The ocr-d processor arguments.
	 * @return The ocr-d arguments for the docker process.
	 * @throws JsonProcessingException Throws on processing (parsing, generating)
	 *                                 JSON arguments that are not pure I/O
	 *                                 problems.
	 * @since 1.8
	 */
	protected List<String> getProcessorArguments(Framework framework, boolean isResources, Object arguments,
			MetsFileGroup metsFileGroup) throws JsonProcessingException {
		// Get the effective system user/group id
		String uid = framework.getValue(ServiceProviderCollection.uid);
		if (uid == null && framework.isUID())
			uid = "" + framework.getUID();

		if (uid != null) {
			String gid = framework.getValue(ServiceProviderCollection.gid);
			if (gid == null && framework.isGID())
				gid = "" + framework.getGID();

			if (gid != null)
				uid += ":" + gid;
		}

		// Build the processor arguments
		List<String> processorArguments = new ArrayList<>(Arrays.asList("run", "--rm"));

		if (uid != null)
			processorArguments.addAll(Arrays.asList("-u", uid));

		if (isResources) {
			Path optResources = getOptResources(framework);

			if (Files.isDirectory(optResources))
				processorArguments.addAll(Arrays.asList("-v",
						optResources.toString() + ":" + framework.getValue(ServiceProviderCollection.dockerResources)));
		}

		processorArguments.addAll(Arrays.asList("-v", framework.getProcessorWorkspace().toString() + ":/data", "-w",
				"/data", "--", framework.getValue(ServiceProviderCollection.dockerImage), processorName(), "-I",
				metsFileGroup.getInput(), "-O", metsFileGroup.getOutput()));

		if (arguments != null)
			processorArguments.addAll(Arrays.asList("-p", objectMapper.writeValueAsString(arguments)));

		return processorArguments;
	}

	/**
	 * Updates the processor messages.
	 * 
	 * @param process        The system process.
	 * @param standardOutput The callback for standard output.
	 * @param standardError  The callback for standard error.
	 * @since 1.8
	 */
	protected void updateProcessorMessages(SystemProcess process, Message standardOutput, Message standardError) {
		if (process != null) {
			String message = process.getStandardOutput();
			if (!message.isBlank())
				standardOutput.update(message.trim());

			message = process.getStandardError();
			if (!message.isBlank())
				standardError.update(message.trim());
		}
	}

	/**
	 * Returns the files walking the file tree rooted at a given starting folder.
	 * 
	 * @param folder    The starting folder.
	 * @param extension If non null, filter the files with this extension. The upper
	 *                  and lower case does not matter.
	 * @return The files walking the file tree rooted at a given starting folder.
	 * @throws IOException Throws if an I/O error is thrown when accessing the
	 *                     starting file.
	 * @since 1.8
	 */
	protected static List<Path> getFiles(Path folder, String extension) throws IOException {
		final String fileExtension = extension == null || extension.isBlank() ? null
				: "." + extension.toLowerCase().trim();

		try (Stream<Path> walk = Files.walk(folder)) {
			return walk.filter(p -> {
				return !Files.isDirectory(p)
						&& (fileExtension == null || p.toString().toLowerCase().endsWith(fileExtension));
			}).collect(Collectors.toList());
		}
	}

	/**
	 * Runs the ocr-d processor without resources folder.
	 * 
	 * @param framework            The framework.
	 * @param arguments            The processor arguments.
	 * @param unnecessaryArguments The unnecessary processor arguments. Null or
	 *                             empty means that there are no unnecessary
	 *                             processor arguments.
	 * @param event                The callback for processor events.
	 * @param standardOutput       The callback for standard output.
	 * @param standardError        The callback for standard error.
	 * @param progress             The callback for progress.
	 * @param baseProgress         The base progress.
	 * @return The processor execution state.
	 * @since 1.8
	 */
	protected ProcessServiceProvider.Processor.State run(Framework framework, Object arguments,
			Set<String> unnecessaryArguments, ProcessorEvent event, Message standardOutput, Message standardError,
			Progress progress, float baseProgress) {

		return run(framework, false, arguments, unnecessaryArguments, event, standardOutput, standardError, progress,
				baseProgress);
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
	 * @param event                The callback for processor events.
	 * @param standardOutput       The callback for standard output.
	 * @param standardError        The callback for standard error.
	 * @param progress             The callback for progress.
	 * @param baseProgress         The base progress.
	 * @return The processor execution state.
	 * @since 1.8
	 */
	protected ProcessServiceProvider.Processor.State run(Framework framework, boolean isResources, Object arguments,
			Set<String> unnecessaryArguments, ProcessorEvent event, Message standardOutput, Message standardError,
			Progress progress, float baseProgress) {
		try {
			standardOutput.update("Using processor parameters " + objectMapper.writeValueAsString(arguments) + ".");
		} catch (JsonProcessingException ex) {
			standardError.update("Troubles creating JSON from parameters - " + ex.getMessage() + ".");
		}

		if (unnecessaryArguments != null && !unnecessaryArguments.isEmpty())
			standardOutput.update("Ignored unnecessary parameters: " + unnecessaryArguments + ".");

		if (event.isCanceled())
			return ProcessServiceProvider.Processor.State.canceled;

		progress.update(baseProgress);

		// Processor workspace path
		final Path processorWorkspaceRelativePath = framework.getOutputRelativeProcessorWorkspace();
		if (processorWorkspaceRelativePath == null) {
			standardError.update("Inconsistent processor workspace path.");

			return ProcessServiceProvider.Processor.State.interrupted;
		}

		final Path metsPath = framework.getMets();
		if (metsPath == null) {
			standardError.update("Missed required mets file path.");

			return ProcessServiceProvider.Processor.State.interrupted;
		}

		final MetsFileGroup metsFileGroup = new MetsFileGroup(framework);

		SystemProcess preprocess = null;
		try {
			List<String> processorArguments = getProcessorArguments(framework, isResources, arguments, metsFileGroup);

			preprocess = getDockerProcess(framework);

			standardOutput.update("Execute docker process '" + preprocess.getCommand() + "' with parameters: "
					+ processorArguments + ".");

			preprocess.execute(processorArguments);

			updateProcessorMessages(preprocess, standardOutput, standardError);

			if (preprocess.getExitValue() != 0) {
				standardError.update("Cannot run " + processorDescription() + ".");

				return ProcessServiceProvider.Processor.State.interrupted;
			}
		} catch (IOException e) {
			updateProcessorMessages(preprocess, standardOutput, standardError);

			standardError.update("troubles running " + processorDescription() + " - " + e.getMessage() + ".");

			return ProcessServiceProvider.Processor.State.interrupted;
		}

		if (event.isCanceled())
			return ProcessServiceProvider.Processor.State.canceled;

		progress.update(0.097F);

		// Update paths in xml files
		standardOutput.update("Update paths in xml files.");
		try {
			for (Path file : getFiles(
					Paths.get(framework.getProcessorWorkspace().toString(), metsFileGroup.getOutput()), "xml"))
				try (Stream<String> lines = Files.lines(file)) {
					Files.write(file,
							lines.map(line -> line.replace("=\"" + metsFileGroup.getOutput() + "/",
									"=\"" + processorWorkspaceRelativePath.toString() + "/"))
									.collect(Collectors.joining("\n")).getBytes());
				}
		} catch (IOException e) {
			standardError
					.update("troubles updating " + processorDescription() + " xml files - " + e.getMessage() + ".");

			return ProcessServiceProvider.Processor.State.interrupted;
		}

		progress.update(0.098F);

		// Move processor output directory to snapshot sandbox
		standardOutput.update("Move processor output directory to snapshot sandbox.");
		try {
			Files.move(Paths.get(framework.getProcessorWorkspace().toString(), metsFileGroup.getOutput()),
					framework.getOutput(), StandardCopyOption.REPLACE_EXISTING);

		} catch (IOException e) {
			standardError.update("troubles moving " + processorDescription()
					+ " output directory to snapshot sandbox - " + e.getMessage() + ".");

			return ProcessServiceProvider.Processor.State.interrupted;
		}

		if (event.isCanceled())
			return ProcessServiceProvider.Processor.State.canceled;

		progress.update(0.099F);

		// Update paths in mets file
		standardOutput.update("Update paths in mets file.");
		try (Stream<String> lines = Files.lines(metsPath)) {
			Files.write(metsPath,
					lines.map(line -> line.replace("=\"" + metsFileGroup.getOutput() + "/",
							"=\"" + processorWorkspaceRelativePath.toString() + "/")).collect(Collectors.joining("\n"))
							.getBytes());
		} catch (IOException e) {
			standardError
					.update("troubles updating " + processorDescription() + " mets file - " + e.getMessage() + ".");

			return ProcessServiceProvider.Processor.State.interrupted;
		}

		progress.update(1F);
		standardOutput.update(processorName() + " completed.");

		return ProcessServiceProvider.Processor.State.completed;
	}

	/**
	 * Returns the opt folder.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @param folders       The sub folders.
	 * @return The opt folder.
	 * @since 1.8
	 */
	private Path getOptFolder(ConfigurationServiceProvider configuration, Target target,
			Framework.ServiceProviderCollectionKey... folders) {
		if (configuration == null || target == null || target.getOpt() == null)
			return null;

		Path optPath = Paths
				.get(target.getOpt().toString(), Framework.getValue(configuration, ServiceProviderCollection.optFolder))
				.normalize();

		Path subPath = optPath;
		for (Framework.ServiceProviderCollectionKey folder : folders)
			subPath = Paths.get(subPath.toString(), Framework.getValue(configuration, folder));

		subPath = subPath.normalize();

		return subPath.startsWith(optPath) ? subPath : optPath;
	}

	/**
	 * Returns the opt resources folder.
	 * 
	 * @param framework The framework.
	 * @return The opt resources folder.
	 * @since 1.8
	 */
	protected Path getOptResources(Framework framework) {
		return framework == null ? null : getOptResources(framework.getConfiguration(), framework.getTarget());
	}

	/**
	 * Returns the opt resources folder.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @return The opt resources folder.
	 * @since 1.8
	 */
	protected Path getOptResources(ConfigurationServiceProvider configuration, Target target) {
		return getOptFolder(configuration, target, ServiceProviderCollection.optResources);
	}

	/**
	 * Returns the docker resources folder.
	 * 
	 * @param framework The framework.
	 * @return The docker resources folder.
	 * @since 1.8
	 */
	protected Path getDockerResources(Framework framework) {
		return framework == null ? null : getDockerResources(framework.getConfiguration());
	}

	/**
	 * Returns the docker resources folder.
	 * 
	 * @param configuration The service provider configuration.
	 * @return The docker resources folder.
	 * @since 1.8
	 */
	protected Path getDockerResources(ConfigurationServiceProvider configuration) {
		return Paths.get(Framework.getValue(configuration, ServiceProviderCollection.dockerResources)).normalize();
	}

	/**
	 * Returns the directories in given folder.
	 * 
	 * @param folder The folder to returns the directories.
	 * @return The directories in given folder. If can not open the folder, then
	 *         returns an empty list.
	 * @since 1.8
	 */
	public static List<Path> getDirectories(Path folder) {
		try {
			return Files.list(folder).filter(file -> Files.isDirectory(file)).collect(Collectors.toList());
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	/**
	 * Defines callback for messages.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	@FunctionalInterface
	protected interface Message {
		/**
		 * Updates the message.
		 * 
		 * @param content The message content.
		 * @since 1.8
		 */
		public void update(String content);
	}

	/**
	 * Defines callback for progresses.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	@FunctionalInterface
	protected interface Progress {
		/**
		 * Updates the progress.
		 * 
		 * @param value The progress value.
		 * @since 1.8
		 */
		public void update(float value);
	}

	/**
	 * Defines callback for processor events.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	@FunctionalInterface
	protected interface ProcessorEvent {
		/**
		 * Returns true if the was canceled.
		 * 
		 * @return True if the was canceled.
		 * @since 1.8
		 */
		public boolean isCanceled();
	}

	/**
	 * MetsFileGroup is an immutable class that defines mets file groups.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	protected static class MetsFileGroup {
		/**
		 * The file group prefix.
		 */
		private final String fileGroupPrefix;

		/**
		 * The input file group.
		 */
		private final String input;

		/**
		 * The output file group.
		 */
		private final String output;

		/**
		 * Creates a mets file group.
		 * 
		 * @param framework The framework.
		 * @since 1.8
		 */
		public MetsFileGroup(Framework framework) {
			super();

			fileGroupPrefix = framework.getMetsGroup();

			this.input = getFileGroup(framework.getTarget().getWorkflow().getSnapshotTrack());
			this.output = getFileGroup(framework.getSnapshotTrack());
		}

		/**
		 * Returns the file group.
		 * 
		 * @param snapshotTrack The snapshot track.
		 * @return The file group.
		 * @since 1.8
		 */
		private String getFileGroup(List<Integer> snapshotTrack) {
			StringBuffer path = new StringBuffer();

			for (int snapshotId : snapshotTrack)
				path.append("-" + snapshotId);

			return fileGroupPrefix + path.toString();
		}

		/**
		 * Returns the file group prefix.
		 *
		 * @return The file group prefix.
		 * @since 1.8
		 */
		public String getFileGroupPrefix() {
			return fileGroupPrefix;
		}

		/**
		 * Returns the input file group.
		 *
		 * @return The input file group.
		 * @since 1.8
		 */
		public String getInput() {
			return input;
		}

		/**
		 * Returns the output file group.
		 *
		 * @return The output file group.
		 * @since 1.8
		 */
		public String getOutput() {
			return output;
		}

	}
}
