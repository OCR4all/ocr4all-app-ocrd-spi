/**
 * File:     ProviderDescription.java
 * Package:  de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util
 * 
 * Author:   Herbert Baier (herbert.baier@uni-wuerzburg.de)
 * Date:     28.02.2024
 */
package de.uniwuerzburg.zpd.ocr4all.application.ocrd.spi.util;

import java.security.ProviderException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uniwuerzburg.zpd.ocr4all.application.spi.model.BooleanField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.DecimalField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Entry;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Field;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.IntegerField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.Model;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.SelectField;
import de.uniwuerzburg.zpd.ocr4all.application.spi.model.StringField;

/**
 * Defines provider descriptions.
 *
 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
 * @version 1.0
 * @since 17
 */
public class ProviderDescription {
	/**
	 * The JSON object mapper.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();
	{
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * The JSON processor description.
	 */
	private final String json;

	/**
	 * The service provider description.
	 */
	private final String description;

	/**
	 * The service provider categories.
	 */
	private final List<String> categories;

	/**
	 * The service provider steps.
	 */
	private final List<String> steps;

	/**
	 * The model factory.
	 */
	private final ModelFactory modelFactory;

	/**
	 * Creates the provider description.
	 * 
	 * @param json The JSON processor description.
	 * @throws ProviderException Throws if the JSON processor description can not be
	 *                           parsed.
	 * @since 17
	 */
	public ProviderDescription(String json) throws ProviderException {
		if (!json.isBlank()) {
			this.json = json.trim();

			try {
				final JsonNode root = objectMapper.readTree(this.json);

				JsonNode descriptionNode = root.get("description");
				description = descriptionNode == null || descriptionNode.isContainerNode() ? null
						: descriptionNode.asText();

				categories = getValueAsList("categories", root);
				steps = getValueAsList("steps", root);

				modelFactory = new ModelFactory(root);
			} catch (JsonProcessingException e) {
				throw new ProviderException("could not parse JSON processor description - " + e.getMessage());
			}
		} else {
			this.json = null;
			description = null;
			categories = null;
			steps = null;
			modelFactory = null;
		}
	}

	/**
	 * Returns the json list.
	 * 
	 * @param name       The name of the field (of Object node) to access.
	 * @param objectNode The json object node.
	 * @return The json list.
	 * @since 1.8
	 */
	private List<String> getValueAsList(String name, JsonNode objectNode) {
		JsonNode valuesNode = objectNode.get(name);
		if (valuesNode == null)
			return null;
		else {
			List<String> list = new ArrayList<>();

			for (JsonNode valueNode : valuesNode)
				list.add(valueNode.asText());

			return list;
		}
	}

	/**
	 * Returns true if the JSON processor description is set.
	 *
	 * @return True if the JSON processor description is set.
	 * @since 17
	 */
	public boolean isJsonSet() {
		return json != null;
	}

	/**
	 * Returns the JSON processor description.
	 *
	 * @return The JSON processor description.
	 * @since 17
	 */
	public String getJson() {
		return json;
	}

	/**
	 * Returns true if the service provider description is set.
	 *
	 * @return True if the service provider description is set.
	 * @since 17
	 */
	public boolean isDescriptionSet() {
		return description != null;
	}

	/**
	 * Returns the service provider description.
	 *
	 * @return The service provider description.
	 * @since 17
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns true if there are service provider categories available.
	 *
	 * @return True if there are service provider categories available.
	 * @since 17
	 */
	public boolean isCategoriesAvailable() {
		return categories != null && !categories.isEmpty();
	}

	/**
	 * Returns the service provider categories.
	 *
	 * @return The service provider categories.
	 * @since 17
	 */
	public List<String> getCategories() {
		return categories;
	}

	/**
	 * Returns true if there are service provider steps available.
	 *
	 * @return True if there are service provider steps available.
	 * @since 17
	 */
	public boolean isStepsAvailable() {
		return steps != null && !steps.isEmpty();
	}

	/**
	 * Returns the service provider steps.
	 *
	 * @return The service provider steps.
	 * @since 17
	 */
	public List<String> getSteps() {
		return steps;
	}

	/**
	 * Returns true if the model factory is set.
	 *
	 * @return True if the model factory is set.
	 * @since 17
	 */
	public boolean isModelFactorySet() {
		return modelFactory != null;
	}

	/**
	 * Returns the model factory.
	 *
	 * @return The model factory.
	 * @since 17
	 */
	public ModelFactory getModelFactory() {
		return modelFactory;
	}

	/**
	 * Defines model factories.
	 *
	 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
	 * @version 1.0
	 * @since 17
	 */
	public static class ModelFactory {
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
			 *         represented as Java float.protected
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
		 * A functional interface that allows implementing classes to handle model
		 * fields.
		 *
		 * @author <a href="mailto:herbert.baier@uni-wuerzburg.de">Herbert Baier</a>
		 * @version 1.0
		 * @since 1.8
		 */
		@FunctionalInterface
		public interface ModelFieldCallback {
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
						if (item != null && !item.trim().isEmpty())
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
//					if (format == null) {
//						String name = JsonParameterFiel.format.asText(node);
				//
//						throw new ProviderException(
//								(name == null ? "undefined required format" : "unknown format '" + name + "'")
//										+ " for parameter '" + parameter + "'.");
//					}

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
