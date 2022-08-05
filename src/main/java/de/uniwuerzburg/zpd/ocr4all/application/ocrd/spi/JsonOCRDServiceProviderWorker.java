/**
 * File:     JsonOCRDServiceProviderWorker.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     08.07.2022
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi;

import java.security.ProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.uniwuerzburg.zpd.ocr4all.application.spi.core.CoreProcessorServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.core.ProcessServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Framework;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Premise;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.SystemCommand;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.BooleanField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.DecimalField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Entry;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Field;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.IntegerField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.Argument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.BooleanArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.DecimalArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.IntegerArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.ModelArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.SelectArgument;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.argument.StringArgument;
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
public abstract class JsonOCRDServiceProviderWorker extends OCRDServiceProviderWorker
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
	private final String name;

	/**
	 * The service provider name.
	 */
	private final boolean isResources;

	/**
	 * The ocr-d JSON processor description.
	 */
	protected String jsonProcessorDescription = null;

	/**
	 * The service provider description.
	 */
	private String description = null;

	/**
	 * The model factory.
	 */
	private ModelFactory modelFactory = null;

	/**
	 * Creates an ocr-d service provider worker with JSON support and without
	 * resources.
	 * 
	 * @param name The service provider name.
	 * @since 1.8
	 */
	public JsonOCRDServiceProviderWorker(String name) {
		this(name, false);
	}

	/**
	 * Creates an ocr-d service provider worker with JSON support.
	 * 
	 * @param name        The service provider name.
	 * @param isResources True if resources folder is required.
	 * @since 1.8
	 */
	public JsonOCRDServiceProviderWorker(String name, boolean isResources) {
		this(null, name, isResources);
	}

	/**
	 * Creates an ocr-d service provider worker with JSON support.
	 * 
	 * @param resourceBundleKeyPrefix The prefix of the keys in the resource bundle.
	 * @param name                    The service provider name.
	 * @param isResources             True if resources folder is required.
	 * @since 1.8
	 */
	public JsonOCRDServiceProviderWorker(String resourceBundleKeyPrefix, String name, boolean isResources) {
		super(resourceBundleKeyPrefix);

		this.name = name;
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
		return name;
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
			initializeJSON(process.getStandardOutput());
		else
			throw new ProviderException(
					process.getStandardError().trim() + " (process exit code " + process.getExitValue() + ")");
	}

	/**
	 * Initializes the provider using the JSON processor description.
	 * 
	 * @param json The JSON processor description.
	 * @since 1.8
	 */
	private void initializeJSON(String json) throws ProviderException {
		if (!json.isBlank()) {
			jsonProcessorDescription = json.trim();

			try {
				final JsonNode root = objectMapper.readTree(jsonProcessorDescription);

				final JsonNode descriptionNode = root.get("description");
				description = descriptionNode == null || descriptionNode.isContainerNode() ? null
						: descriptionNode.asText();

				modelFactory = new ModelFactory(root);
			} catch (JsonProcessingException e) {
				throw new ProviderException("could not parse JSON processor description - " + e.getMessage());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProviderCore#
	 * startCallback()
	 */
	@Override
	public void startCallback() throws ProviderException {
		if (modelFactory == null)
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
		return description == null ? super.getDescription(locale) : Optional.of(description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getAdvice()
	 */
	@Override
	public String getAdvice() {
		return jsonProcessorDescription == null ? null : "JSON processor description:\n" + jsonProcessorDescription;
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
	protected Hashtable<String, ModelFieldCallback> getModelCallbacks(Target target, List<String> arguments) {
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
		return modelFactory == null ? null
				: modelFactory.getModel(preModelEntries(target, modelFactory.getArguments()),
						posModelEntries(target, modelFactory.getArguments()),
						getModelCallbacks(target, modelFactory.getArguments()));
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
	protected Hashtable<String, ModelArgumentCallback> getProcessorCallbacks(CoreProcessorServiceProvider processor,
			List<String> arguments) {
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
		return modelFactory == null ? null : new CoreProcessorServiceProvider() {
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
				 * The JSON processor arguments
				 */
				final ObjectNode processorArguments = objectMapper.createObjectNode();

				/*
				 * Parse the processor arguments
				 */
				updatedStandardOutput("Parse parameters.");

				final Set<String> jsonTypeObjectProcessorParameters = modelFactory
						.getJsonTypeObjectProcessorParameters();

				List<Argument> arguments = new ArrayList<>();

				List<Argument> extraArguments = extraArguments(this, modelFactory.getArguments());
				if (extraArguments != null && !extraArguments.isEmpty())
					for (Argument argument : extraArguments)
						if (argument != null)
							arguments.add(argument);

				final Hashtable<String, ModelArgumentCallback> processorCallbacks = getProcessorCallbacks(this,
						modelFactory.getArguments());

				for (Argument argument : modelArgument.getArguments()) {
					if (processorCallbacks != null && processorCallbacks.containsKey(argument.getArgument())) {
						List<Argument> handledArguments = processorCallbacks.get(argument.getArgument())
								.handle(argument, jsonTypeObjectProcessorParameters);

						if (handledArguments != null && !handledArguments.isEmpty())
							for (Argument handled : handledArguments)
								if (handled != null)
									arguments.add(handled);
					} else
						arguments.add(argument);
				}

				// Processes only the arguments that belong to the json description
				Set<String> jsonArguments = new HashSet<>(modelFactory.getArguments());
				for (Argument argument : arguments)
					if (jsonArguments.contains(argument.getArgument())) {
						if (argument instanceof SelectArgument) {
							SelectArgument select = (SelectArgument) argument;

							if (select.getValues().isPresent()) {
								List<String> values = select.getValues().get();

								if (values.size() == 1)
									processorArguments.put(select.getArgument(), values.get(0));
								else if (values.size() > 1) {
									updatedStandardError("The select argument '" + select.getArgument()
											+ "' allows only one value.");

									return ProcessServiceProvider.Processor.State.interrupted;
								}
							}
						} else if (argument instanceof StringArgument) {
							StringArgument string = (StringArgument) argument;

							if (string.getValue().isPresent()) {
								if (jsonTypeObjectProcessorParameters.contains(string.getArgument())) {
									try {
										processorArguments.set(string.getArgument(),
												objectMapper.readTree(string.getValue().get()));
									} catch (JsonProcessingException e) {
										updatedStandardError("The JSON value of argument '" + string.getArgument()
												+ "' can not be parsed - " + e.getMessage());

										return ProcessServiceProvider.Processor.State.interrupted;
									}
								} else
									processorArguments.put(string.getArgument(), string.getValue().get());
							}

						} else if (argument instanceof IntegerArgument) {
							IntegerArgument integer = (IntegerArgument) argument;

							if (integer.getValue().isPresent())
								processorArguments.put(integer.getArgument(), (int) integer.getValue().get());
						} else if (argument instanceof DecimalArgument) {
							DecimalArgument decimal = (DecimalArgument) argument;

							if (decimal.getValue().isPresent())
								processorArguments.put(decimal.getArgument(), (float) decimal.getValue().get());
						} else if (argument instanceof BooleanArgument) {
							BooleanArgument bool = (BooleanArgument) argument;

							if (bool.getValue().isPresent())
								processorArguments.put(bool.getArgument(), (boolean) bool.getValue().get());
						} else {
							updatedStandardError("The argument of type '" + argument.getClass().getName() + "'"
									+ " is not supported.");

							return ProcessServiceProvider.Processor.State.interrupted;
						}
					}

				/*
				 * Runs the processor
				 */
				return run(framework, isResources, processorArguments, null, () -> isCanceled(), () -> complete(),
						message -> updatedStandardOutput(message), message -> updatedStandardError(message),
						progress -> callback.updatedProgress(progress), 0.01F);
			}
		};
	}

	/**
	 * A functional interface that allows implementing classes to handle model
	 * fields.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	@FunctionalInterface
	protected interface ModelFieldCallback {
		/**
		 * Handles the field.
		 * 
		 * @param field The field to handle.
		 * @return The handled fields. Null or empty if the field should be removed.
		 * @since 1.8
		 */
		public List<Field<?>> handle(Field<?> field);
	}

	/**
	 * A functional interface that allows implementing classes to handle model
	 * arguments.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	@FunctionalInterface
	protected interface ModelArgumentCallback {
		/**
		 * Handles the argument.
		 * 
		 * @param argument                          The argument to handle.
		 * @param jsonTypeObjectProcessorParameters The JSON processor parameters of
		 *                                          type object.
		 * @return The handled arguments. Null or empty if the argument should be
		 *         ignored.
		 * @since 1.8
		 */
		public List<Argument> handle(Argument argument, Set<String> jsonTypeObjectProcessorParameters);
	}

	/**
	 * Defines model factories.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 1.8
	 */
	protected static class ModelFactory {
		/**
		 * the JSON content type.
		 */
		private static final String jsonContentType = "application/json";

		/**
		 * Defines JSON parameter fields.
		 *
		 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
		 * @version 1.0
		 * @since 1.8
		 */
		private enum JsonParameterFiel {
			type, description, def("default"), enumeration("enum"), format, contentType("content-type");

			/**
			 * The name.
			 */
			private final String name;

			/**
			 * Default constructor for a JSON parameter field with the name of this enum
			 * constant, exactly as declared in its enum declaration.
			 * 
			 * @since 1.8
			 */
			private JsonParameterFiel() {
				name = name();
			}

			/**
			 * Creates a JSON parameter field with given name.
			 * 
			 * @param name The name.
			 * @since 1.8
			 */
			private JsonParameterFiel(String name) {
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

			/**
			 * Returns true if the field node is available.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available.
			 * @since 1.8
			 */
			public boolean isFieldNode(final JsonNode node) {
				return getFieldNode(node) != null;
			}

			/**
			 * Returns the field node.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The field node.
			 * @since 1.8
			 */
			public JsonNode getFieldNode(final JsonNode node) {
				return node.get(getName());
			}

			/**
			 * Returns true if the field node is available and is an enumeration.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and is an enumeration.
			 * @since 1.8
			 */
			public boolean isEnumeration(final JsonNode node) {
				JsonNode fieldNode = getFieldNode(node);

				if (fieldNode == null || !fieldNode.isArray())
					return false;

				for (JsonNode item : fieldNode)
					if (!item.isTextual())
						return false;

				return true;
			}

			/**
			 * Returns the enumeration if the field node is available and it is an
			 * enumeration.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The enumeration if the field node is available and it is an
			 *         enumeration. Otherwise, returns null.
			 * @since 1.8
			 */
			public List<String> getEnumeration(final JsonNode node) {
				if (isEnumeration(node)) {
					List<String> enumeration = new ArrayList<>();

					for (JsonNode item : getFieldNode(node)) {
						String text = item.asText();

						if (text != null)
							enumeration.add(text);
					}

					return enumeration;

				} else
					return null;
			}

			/**
			 * Returns true if the field node is available and it is an object node.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and it is an object node.
			 * @since 1.8
			 */
			public boolean isObject(final JsonNode node) {
				JsonNode fieldNode = getFieldNode(node);

				return fieldNode != null && fieldNode.isObject();
			}

			/**
			 * Returns a valid String representation of the field value, if the field node
			 * is available and represents basic JSON String value.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The JSON text. Null if the field node is not available or it is not a
			 *         textual node or empty.
			 * @since 1.8
			 */
			public String asObject(final JsonNode node) {
				if (isObject(node)) {
					String text = getFieldNode(node).toString();

					return "{}".equals(text.replaceAll("[\\s]", "")) ? null : text;
				} else
					return null;
			}

			/**
			 * Returns true if the field node is available and represents basic JSON String
			 * value.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and represents basic JSON String
			 *         value.
			 * @since 1.8
			 */
			public boolean isText(final JsonNode node) {
				JsonNode fieldNode = getFieldNode(node);

				return fieldNode != null && fieldNode.isTextual();
			}

			/**
			 * Returns a valid String representation of the field value, if the field node
			 * is available and represents basic JSON String value.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The JSON text. Null if the field node is not available or it is not a
			 *         textual node or empty.
			 * @since 1.8
			 */
			public String asText(final JsonNode node) {
				if (isText(node)) {
					String text = getFieldNode(node).asText();

					return text == null || text.isEmpty() ? null : text;
				} else
					return null;
			}

			/**
			 * Returns true if the field node is available and contained value is a number
			 * represented as Java int.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and contained value is a number
			 *         represented as Java int.
			 * @since 1.8
			 */
			public boolean isInteger(final JsonNode node) {
				JsonNode fieldNode = getFieldNode(node);

				return fieldNode != null && fieldNode.isInt();
			}

			/**
			 * Returns the value of the field node converted to a Java int.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The value of the field node converted to a Java int. 0 on troubles.
			 * @since 1.8
			 */
			public int asInteger(final JsonNode node) {
				return isInteger(node) ? getFieldNode(node).asInt() : 0;
			}

			/**
			 * Returns true if the field node is available and contained value is a number
			 * represented as Java float.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and contained value is a number
			 *         represented as Java float.
			 * @since 1.8
			 */
			public boolean isDecimal(final JsonNode node) {
				JsonNode fieldNode = getFieldNode(node);

				return fieldNode != null && (fieldNode.isDouble() || fieldNode.isInt());
			}

			/**
			 * Returns the value of the field node converted to a Java float.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The value of the field node converted to a Java float. 0 on troubles.
			 * @since 1.8
			 */
			public float asDecimal(final JsonNode node) {
				return isDecimal(node) ? (float) getFieldNode(node).asDouble() : 0;
			}

			/**
			 * Returns true if the field node is available and was created from JSON boolean
			 * value (literals "true" and "false").
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and was created from JSON boolean
			 *         value (literals "true" and "false").
			 * @since 1.8
			 */
			public boolean isBoolean(final JsonNode node) {
				JsonNode fieldNode = getFieldNode(node);

				return fieldNode != null && fieldNode.isBoolean();
			}

			/**
			 * Returns true if the field node is available and was created from JSON boolean
			 * true value (literal "true").
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return True if the field node is available and was created from JSON boolean
			 *         true value (literal "true").
			 * @since 1.8
			 */
			public boolean asBoolean(final JsonNode node) {
				return isBoolean(node) ? getFieldNode(node).asBoolean() : false;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Enum#toString()
			 */
			@Override
			public String toString() {
				return getName();
			}

		}

		/**
		 * Defines JSON field types.
		 *
		 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
		 * @version 1.0
		 * @since 1.8
		 */
		private enum JsonFieldType {
			string, number, bool("boolean"), object;

			/**
			 * The name.
			 */
			private final String name;

			/**
			 * Default constructor for a JSON field type with the name of this enum
			 * constant, exactly as declared in its enum declaration.
			 * 
			 * @since 1.8
			 */
			private JsonFieldType() {
				name = name();
			}

			/**
			 * Creates a JSON field type with given name.
			 * 
			 * @param name The name.
			 * @since 1.8
			 */
			private JsonFieldType(String name) {
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

			/**
			 * Returns the JSON field type for given node.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The JSON field type. Null if unknown.
			 * @since 1.8
			 */
			public static JsonFieldType getType(JsonNode node) {
				String name = JsonParameterFiel.type.asText(node);

				if (name != null && !name.isBlank()) {
					name = name.trim().toLowerCase();
					for (JsonFieldType type : JsonFieldType.values())
						if (type.getName().equals(name))
							return type;
				}

				return null;
			}

		}

		/**
		 * Defines JSON field number formats.
		 *
		 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
		 * @version 1.0
		 * @since 1.8
		 */
		private enum JsonFieldNumberFormat {
			integer, integerShortForm("int"), decimal("float");

			/**
			 * The name.
			 */
			private final String name;

			/**
			 * Default constructor for a JSON field number format with the name of this enum
			 * constant, exactly as declared in its enum declaration.
			 * 
			 * @since 1.8
			 */
			private JsonFieldNumberFormat() {
				name = name();
			}

			/**
			 * Creates a JSON field number format with given name.
			 * 
			 * @param name The name.
			 * @since 1.8
			 */
			private JsonFieldNumberFormat(String name) {
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

			/**
			 * Returns the JSON field number format for given JSON node.
			 * 
			 * @param node The JSON object node for the processor parameter.
			 * @return The JSON field number format. Decimal if unknown.
			 * @since 1.8
			 */
			public static JsonFieldNumberFormat getFormat(JsonNode node) {
				String name = JsonParameterFiel.format.asText(node);

				if (name != null && !name.isBlank()) {
					name = name.trim().toLowerCase();
					for (JsonFieldNumberFormat type : JsonFieldNumberFormat.values())
						if (type.getName().equals(name))
							return type;
				}

				return decimal;
			}

		}

		/**
		 * The model fields.
		 */
		private final List<Field<?>> fields = new ArrayList<>();

		/**
		 * The JSON processor parameters of type object.
		 */
		private final Set<String> jsonTypeObjectProcessorParameters = new HashSet<>();

		/**
		 * Creates a model factory.
		 * 
		 * @param root The JSON object node for the ocr-d processor.
		 * @throws ProviderException Throws if the JSON node for the processor
		 *                           parameters is not an object.
		 * @since 1.8
		 */
		private ModelFactory(final JsonNode root) throws ProviderException {
			super();

			JsonNode node = root.get("parameters");

			if (node != null) {
				if (!node.isObject())
					throw new ProviderException(
							"expecting type object for JSON processor parameters, but it is of type "
									+ node.getNodeType().name().toLowerCase() + ".");

				Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

				while (fields.hasNext()) {
					Map.Entry<String, JsonNode> field = fields.next();

					this.fields.add(parse(field.getKey(), field.getValue()));
				}
			}
		}

		/**
		 * Returns the model.
		 * 
		 * @param preModelEntries     The entries to be added at the beginning of the
		 *                            model. Null or empty if no entry is required.
		 * @param posModelEntries     The entries to be added at the end of the model.
		 *                            Null or empty if no entry is required.
		 * @param modelFieldCallbacks The model fields that need to be handled. The key
		 *                            is the field argument and the value the desired
		 *                            field handler.
		 * @return The model.
		 * @since 1.8
		 */
		public Model getModel(List<Entry> preModelEntries, List<Entry> posModelEntries,
				Hashtable<String, ModelFieldCallback> modelFieldCallbacks) {
			List<Entry> entries = new ArrayList<>();

			if (preModelEntries != null && !preModelEntries.isEmpty())
				for (Entry entry : preModelEntries)
					if (entry != null)
						entries.add(entry);

			for (Field<?> field : fields) {
				if (modelFieldCallbacks != null && modelFieldCallbacks.containsKey(field.getArgument())) {
					List<Field<?>> fields = modelFieldCallbacks.get(field.getArgument()).handle(field);

					if (fields != null && !fields.isEmpty())
						for (Field<?> handled : fields)
							if (handled != null)
								entries.add(handled);
				} else
					entries.add(field);
			}

			if (posModelEntries != null && !posModelEntries.isEmpty())
				for (Entry entry : posModelEntries)
					if (entry != null)
						entries.add(entry);

			return new Model(entries);
		}

		/**
		 * Returns the arguments in reading order.
		 * 
		 * @return The arguments in reading order.
		 * @since 1.8
		 */
		public List<String> getArguments() {
			List<String> arguments = new ArrayList<>();

			for (Field<?> field : fields)
				arguments.add(field.getArgument());

			return arguments;
		}

		/**
		 * Returns the JSON processor parameters of type object.
		 *
		 * @return The JSON processor parameters of type object.
		 * @since 1.8
		 */
		public Set<String> getJsonTypeObjectProcessorParameters() {
			return new HashSet<>(jsonTypeObjectProcessorParameters);
		}

		/**
		 * Parses the JSON processor parameter and returns the respective service
		 * provider model field.
		 * 
		 * @param parameter The parameter name.
		 * @param node      The JSON object node for the processor parameter.
		 * @return The service provider model field.
		 * @throws ProviderException Throws on parse troubles.
		 * @since 1.8
		 */
		private Field<?> parse(String parameter, JsonNode node) throws ProviderException {
			final JsonFieldType type = JsonFieldType.getType(node);
			if (type == null) {
				String name = JsonParameterFiel.type.asText(node);

				throw new ProviderException((name == null ? "undefined required type" : "unknown type '" + name + "'")
						+ " for parameter '" + parameter + "'.");
			}

			final String description = JsonParameterFiel.description.asText(node);
			switch (type) {
			case string:
				if (JsonParameterFiel.enumeration.isFieldNode(node)) {
					if (!JsonParameterFiel.enumeration.isEnumeration(node))
						throw new ProviderException(
								"the field 'enum' for parameter '" + parameter + "' is not an enumeration.");

					final String value = JsonParameterFiel.def.asText(node);

					final List<SelectField.Item> items = new ArrayList<SelectField.Item>();
					for (String item : JsonParameterFiel.enumeration.getEnumeration(node))
						if (item != null)
							items.add(new SelectField.Option(item.equals(value), item, null));

					return new SelectField(parameter, locale -> parameter, locale -> description, false, items, false);
				} else
					return new StringField(parameter, JsonParameterFiel.def.asText(node),
							JsonParameterFiel.contentType.asText(node), locale -> parameter, locale -> description,
							null, false);
			case number:
				JsonFieldNumberFormat format = JsonFieldNumberFormat.getFormat(node);
				/*
				 * Not required, since using decimal as default format.
				 */
//				if (format == null) {
//					String name = JsonParameterFiel.format.asText(node);
//
//					throw new ProviderException(
//							(name == null ? "undefined required format" : "unknown format '" + name + "'")
//									+ " for parameter '" + parameter + "'.");
//				}

				switch (format) {
				case integer:
				case integerShortForm:
					return new IntegerField(parameter,
							JsonParameterFiel.def.isInteger(node) ? JsonParameterFiel.def.asInteger(node) : null,
							locale -> parameter, locale -> description, null, null, null, null, null, false);
				case decimal:
					return new DecimalField(parameter,
							JsonParameterFiel.def.isDecimal(node) ? JsonParameterFiel.def.asDecimal(node) : null,
							locale -> parameter, locale -> description, null, null, null, null, null, false);
				default:
					throw new ProviderException("the format '" + format.getName() + "' for parameter '" + parameter
							+ "' is not implemented.");
				}
			case bool:
				return new BooleanField(parameter,
						JsonParameterFiel.def.isBoolean(node) ? JsonParameterFiel.def.asBoolean(node) : null,
						locale -> parameter, locale -> description, false);
			case object:
				jsonTypeObjectProcessorParameters.add(parameter);

				return new StringField(parameter, JsonParameterFiel.def.asObject(node), jsonContentType,
						locale -> parameter, locale -> description, null, false);
			default:
				throw new ProviderException(
						"the type '" + type.getName() + "' for parameter '" + parameter + "' is not implemented.");
			}
		}

	}
}
