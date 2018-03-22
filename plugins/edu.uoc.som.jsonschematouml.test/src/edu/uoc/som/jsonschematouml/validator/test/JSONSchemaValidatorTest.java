package edu.uoc.som.jsonschematouml.validator.test;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import edu.uoc.som.jsonschematouml.validator.JSONSchemaValidator;
import junit.framework.TestCase;

public class JSONSchemaValidatorTest extends TestCase {
    
    @Test
    public void testValidateValid() {
    	File input = new File("inputs/testValid.json");
        try {
        	ProcessingReport report = JSONSchemaValidator.validate(input);
        	assertTrue(report.isSuccess());
        } catch (IOException | ProcessingException e) {
			fail(e.getLocalizedMessage());
		}
    }
    
    public void testValidateInvalid() {
    	File input = new File("inputs/testInvalid.json");
        try {
        	ProcessingReport report = JSONSchemaValidator.validate(input);
        	assertFalse(report.isSuccess());
        } catch (IOException | ProcessingException e) {
			fail(e.getLocalizedMessage());
		}
    }
}
