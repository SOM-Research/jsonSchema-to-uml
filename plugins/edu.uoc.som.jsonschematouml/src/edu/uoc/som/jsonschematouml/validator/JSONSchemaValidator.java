package edu.uoc.som.jsonschematouml.validator;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;


/**
 * This class validates that a JSON file conforms to the standard JSON Schema specification.
 * 
 * The implementation of this class is inspired by edu.uoc.som.openapitouml.validator.OpenAPIValidator
 * (https://github.com/SOM-Research/openapi-to-uml)
 * 
 */
public class JSONSchemaValidator {
	/**
	 * The Validator used to check that a JSON Schema is valid
	 */
	private static final SyntaxValidator VALIDATOR = new SyntaxValidator(ValidationConfiguration.byDefault());
	
	/**
	 * Validates that a jsonFile conforms to the JSON Schema specification
	 * 
	 * @param jsonFile The file to validate
	 * @return The report of the validation
	 * @throws ProcessingException
	 * @throws IOException
	 */
	public static ProcessingReport validate(File jsonFile) throws ProcessingException, IOException {
		JsonNode jsonNode = JsonLoader.fromFile(jsonFile);
		return VALIDATOR.validateSchema(jsonNode);
	}
}
