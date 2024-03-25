/**
 * File:     OCRDServiceProviderWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     01.03.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util.ProviderDescription;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProviderCore;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Field;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.util.SystemProcess;

/**
 * Defines ocr-d service provider workers. The following properties of the
 * service provider collection <b>ocr-d</b> override the local default settings
 * (<b>key</b>: <i>default value</i>):
 * <ul>
 * <li>opt-folder: ocr-d</li>
 * <li>opt-resources: resources</li>
 * </ul>
 *
 * 
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public abstract class OCRDServiceProviderWorker extends ServiceProviderCore {
	/**
	 * The collection name.
	 */
	protected static final String collectionName = "ocr-d";

	/**
	 * Defines service provider collection with keys and default values. Collection
	 * blank values are not allowed and their values are trimmed.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	private enum ServiceProviderCollection implements ConfigurationServiceProvider.CollectionKey {
		optFolder("opt-folder", "ocr-d"), optResources("opt-resources", "resources");

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
	 * The JSON object mapper.
	 */
	protected final ObjectMapper objectMapper = new ObjectMapper();
	{
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Default constructor for an ocr-d service provider worker.
	 * 
	 * @since 17
	 */
	public OCRDServiceProviderWorker() {
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
		return "ocr-d/" + getProcessorIdentifier();
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

	/**
	 * Returns the service provider collection with key and default value for
	 * processor identifier.
	 * 
	 * @return The service provider collection with key and default value for
	 *         processor identifier.
	 * @since 1.8
	 */
	protected abstract ConfigurationServiceProvider.CollectionKey processorIdentifier();

	/**
	 * Returns the processor identifier.
	 * 
	 * @return The processor identifier.
	 * @since 1.8
	 */
	protected String getProcessorIdentifier() {
		return ConfigurationServiceProvider.getValue(configuration, processorIdentifier());
	}

	/**
	 * Returns the service provider collection with key and default value for
	 * processor description.
	 * 
	 * @return The service provider collection with key and default value for
	 *         processor description.
	 * @since 1.8
	 */
	protected abstract ConfigurationServiceProvider.CollectionKey processorDescription();

	/**
	 * Returns the processor description.
	 * 
	 * @return The processor description.
	 * @since 1.8
	 */
	protected String getProcessorDescription() {
		return ConfigurationServiceProvider.getValue(configuration, processorDescription());
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
	 * Updates the processor messages.
	 * 
	 * @param process        The system process.
	 * @param standardOutput The callback for standard output.
	 * @param standardError  The callback for standard error.
	 * @since 1.8
	 */
	protected static void updateProcessorMessages(SystemProcess process, Message standardOutput,
			Message standardError) {
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
	 * Returns the directories in given folder.
	 * 
	 * @param folder The folder to returns the directories.
	 * @return The directories in given folder. If can not open the folder, then
	 *         returns an empty list.
	 * @since 1.8
	 */
	protected static List<Path> getDirectories(Path folder) {
		try {
			return Files.list(folder).filter(file -> Files.isDirectory(file)).collect(Collectors.toList());
		} catch (Exception e) {
			return new ArrayList<>();
		}
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
			ConfigurationServiceProvider.CollectionKey... folders) {
		if (configuration == null || target == null || target.getOpt() == null)
			return null;

		Path optPath = Paths
				.get(target.getOpt().toString(),
						ConfigurationServiceProvider.getValue(configuration, ServiceProviderCollection.optFolder))
				.normalize();

		Path subPath = optPath;
		for (ConfigurationServiceProvider.CollectionKey folder : folders)
			subPath = Paths.get(subPath.toString(), ConfigurationServiceProvider.getValue(configuration, folder));

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
		return framework == null ? null : getOptResources(configuration, framework.getTarget());
	}

	/**
	 * Returns the opt resources folder.
	 * 
	 * @param framework           The framework.
	 * @param processorIdentifier The service provider collection with key and
	 *                            default value for processor identifier.
	 * @return The opt resources folder.
	 * @since 1.8
	 */
	protected Path getOptResources(Framework framework,
			ConfigurationServiceProvider.CollectionKey processorIdentifier) {
		return framework == null ? null : getOptResources(configuration, framework.getTarget(), processorIdentifier);
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
		return getOptResources(configuration, target, processorIdentifier());
	}

	/**
	 * Returns the opt resources folder.
	 * 
	 * @param configuration       The service provider configuration.
	 * @param target              The target.
	 * @param processorIdentifier The service provider collection with key and
	 *                            default value for processor identifier.
	 * @return The opt resources folder.
	 * @since 1.8
	 */
	protected Path getOptResources(ConfigurationServiceProvider configuration, Target target,
			ConfigurationServiceProvider.CollectionKey processorIdentifier) {
		return getOptFolder(configuration, target, ServiceProviderCollection.optResources, processorIdentifier);
	}

	/**
	 * Returns the folders in opt resources. The hidden folders, this means staring
	 * with a dot, are ignored.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @return The folders in opt resources.
	 * @since 1.8
	 */
	protected List<String> getOptResourcesFolders(ConfigurationServiceProvider configuration, Target target) {
		List<String> models = new ArrayList<>();

		for (Path path : getDirectories(getOptResources(configuration, target)))
			if (!path.getFileName().toString().startsWith("."))
				models.add(path.getFileName().toString());

		Collections.sort(models, String.CASE_INSENSITIVE_ORDER);

		return models;
	}

	/**
	 * Returns the model field callback for opt resource.
	 * 
	 * @param configuration The service provider configuration.
	 * @param target        The target.
	 * @param defaultModel  The default model.
	 * @return The model field callback for opt resource.
	 * @since 1.8
	 */
	protected ProviderDescription.ModelFactory.ModelFieldCallback getOptResourcesFolderFieldCallback(
			ConfigurationServiceProvider configuration, Target target, String defaultModel) {
		return new ProviderDescription.ModelFactory.ModelFieldCallback() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.docker.
			 * OCRDDockerJsonServiceProviderWorker.ModelFieldCallback#handle(de.uniwuerzburg
			 * .zpd. ocr4all.application.spi.model.Field)
			 */
			@Override
			public List<Field<?>> handle(Field<?> field) {
				if (field instanceof StringField) {
					final StringField stringField = ((StringField) field);

					final String value = defaultModel == null ? stringField.getValue().orElse(null) : defaultModel;

					final List<SelectField.Item> models = new ArrayList<SelectField.Item>();
					for (String model : getOptResourcesFolders(configuration, target))
						models.add(new SelectField.Option(model.equals(value), model, null));

					if (models.isEmpty())
						models.add(new SelectField.Option(false, "empty", locale -> "model.empty"));

					return Arrays.asList(new SelectField[] {
							new SelectField(stringField.getArgument(), locale -> stringField.getLabel(locale),
									locale -> stringField.getDescription(locale).orElse(null), false, models, false) });
				} else
					return null;
			}
		};
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
		return getFiles(folder, Integer.MAX_VALUE, extension);
	}

	/**
	 * Returns the files from the top-level folder.
	 * 
	 * @param folder    The starting folder.
	 * @param extension If non null, filter the files with this extension. The upper
	 *                  and lower case does not matter.
	 * @return The files walking the file tree rooted at a given starting folder.
	 * @throws IOException Throws if an I/O error is thrown when accessing the
	 *                     starting file.
	 * @since 1.8
	 */
	protected static List<Path> getFilesTopLevelFolder(Path folder, String extension) throws IOException {
		return getFiles(folder, 1, extension);
	}

	/**
	 * Returns the files walking the file tree rooted at a given starting folder.
	 * 
	 * @param folder    The starting folder.
	 * @param maxDepth  The maximum number of directory levels to visit. A value of
	 *                  0 means that only the starting file is visited. A value of
	 *                  {@link Integer#MAX_VALUE} may be used to indicate that all
	 *                  levels should be visited.
	 * @param extension If non null, filter the files with this extension. The upper
	 *                  and lower case does not matter.
	 * @return The files walking the file tree rooted at a given starting folder.
	 * @throws IOException Throws if an I/O error is thrown when accessing the
	 *                     starting file.
	 * @since 1.8
	 */
	protected static List<Path> getFiles(Path folder, int maxDepth, String extension) throws IOException {
		final String fileExtension = extension == null || extension.isBlank() ? null
				: "." + extension.toLowerCase().trim();

		try (Stream<Path> walk = Files.walk(folder, maxDepth)) {
			return walk.filter(p -> {
				return !Files.isDirectory(p)
						&& (fileExtension == null || p.toString().toLowerCase().endsWith(fileExtension));
			}).collect(Collectors.toList());
		}
	}

}