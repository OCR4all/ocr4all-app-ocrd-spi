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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uniwuerzburg.zpd.ocr4all.application.spi.env.ConfigurationServiceProvider;
import de.uniwuerzburg.zpd.ocr4all.application.spi.env.Target;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.BooleanField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.DecimalField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Entry;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Field;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.IntegerField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;
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
public abstract class JsonOCRDServiceProviderWorker extends OCRDServiceProviderWorker {
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
	 * Default constructor for an ocr-d service provider worker with JSON support.
	 * 
	 * @since 1.8
	 */
	public JsonOCRDServiceProviderWorker() {
		super();
	}

	/**
	 * Creates an ocr-d service provider worker with JSON support.
	 * 
	 * @param resourceBundleKeyPrefix The prefix of the keys in the resource bundle.
	 * @since 1.8
	 */
	public JsonOCRDServiceProviderWorker(String resourceBundleKeyPrefix) {
		super(resourceBundleKeyPrefix);
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

			ObjectMapper mapper = new ObjectMapper();
			try {
				final JsonNode root = mapper.readTree(jsonProcessorDescription);

				description = getText(root.get("description"));

				modelFactory = new ModelFactory(root);
			} catch (JsonProcessingException e) {
				throw new ProviderException("could not parse JSON processor description - " + e.getMessage());
			}
		}
	}

	/**
	 * Returns the JSON text of the given node.
	 * 
	 * @param node The JSON node.
	 * @return The JSON text of the given node. Null if the node is null or it is a
	 *         container.
	 * @since 1.8
	 */
	public static String getText(final JsonNode node) {
		if (node == null || node.isContainerNode())
			return null;
		else {
			return node.asText();
		}
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
	 * de.uniwuerzburg.zpd.ocr4all.application.spi.core.ServiceProvider#getModel(de.
	 * uniwuerzburg.zpd.ocr4all.application.spi.env.Target)
	 */
	@Override
	public Model getModel(Target target) {
		return modelFactory == null ? null : modelFactory.getModel();
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
		 * Defines JSON parameter fields.
		 *
		 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
		 * @version 1.0
		 * @since 1.8
		 */
		private enum JsonParameterFiel {
			type, description, def("default"), enumeration("enum"), format;

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
					if (!isText(item))
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

					for (JsonNode item : getFieldNode(node))
						enumeration.add(asText(item));

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
					String text = getFieldNode(node).asText();

					return text == null || "{}".equals(text.replaceAll("[\\s]", "")) ? null : text;
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

				return fieldNode != null && fieldNode.isFloat();
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
			integer, decimal("float");

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
			 * @return The JSON field number format. Null if unknown.
			 * @since 1.8
			 */
			public static JsonFieldNumberFormat getFormat(JsonNode node) {
				String name = JsonParameterFiel.type.asText(node);

				if (name != null && !name.isBlank()) {
					name = name.trim().toLowerCase();
					for (JsonFieldNumberFormat type : JsonFieldNumberFormat.values())
						if (type.getName().equals(name))
							return type;
				}

				return null;
			}

		}

		/**
		 * The model fields.
		 */
		private final List<Field<?>> fields = new ArrayList<>();

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
		 * @return The model.
		 * @since 1.8
		 */
		public Model getModel() {
			List<Entry> entries = new ArrayList<>();

			for (Field<?> field : fields)
				entries.add(field);

			return new Model(entries);
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

					return new SelectField(parameter, locale -> parameter, locale -> description, true, items, false);
				} else
					return new StringField(parameter, JsonParameterFiel.def.asText(node), locale -> parameter,
							locale -> description, null, false);
			case number:
				JsonFieldNumberFormat format = JsonFieldNumberFormat.getFormat(node);
				if (format == null) {
					String name = JsonParameterFiel.format.asText(node);

					throw new ProviderException(
							(name == null ? "undefined required format" : "unknown format '" + name + "'")
									+ " for parameter '" + parameter + "'.");
				}

				switch (format) {
				case integer:
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
				return new StringField(parameter, JsonParameterFiel.def.asObject(node), locale -> parameter,
						locale -> description, null, false);
			default:
				throw new ProviderException(
						"the type '" + type.getName() + "' for parameter '" + parameter + "' is not implemented.");
			}
		}

	}
}
