package edu.uoc.som.jsonschematouml.generators;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * Entry point for the JSONSchemaToUML tool. You should use this class as a façade for everything provided by the tool.
 *
 * Be aware that the current implementation is just a prototype to validate the feasibility of the idea. Further
 * versions should refine this code and apply good Java practices. For now, this is just a proof-of-concept.
 *
 * The inner workings of this class is pretty straight-forward. Given a {@link File} (which can be a folder or a JSON
 * file), the tool analyzes the document/s according to the JSON schema validation specification and creates the
 * corresponding UML model.
 *
 * @version 0.0.2
 */
public class JSONSchemaToUML {
	/**
	 * The default name to give if no one is provided
	 */
	public static String DEFAULT_MODEL_NAME = "test";
	
    /**
     * This class is used to represent proxy associations (i.e., the type will be resolved later)
     *
     */
    class ProxyAssociation {
        /**
         * The class tha owns this association
         */
        Class owner;
        /**
         * If the association is a composite one (at source/target)
         */
        boolean sourceComposition, targetComposition;
        /**
         * The aggregation kind of the association ends (source/target)
         */
        AggregationKind sourceKind, targetKind;
        /**
         * The name of the association ends
         */
        String sourceEnd, targetEnd;
        /**
         * Cardinalities
         */
        int sourceUpper, sourceLower, targetUpper, targetLower;
    }

    /**
     * The Oracle is used to keep track of every concept (i.e., Class) created.
     * URIs are used to index the elements
     */
    HashMap<String, Class> oracle = new HashMap<>();

    /**
     * The references to classes used as superclasses found during the analysis
     * (to be later resolved by {@link #resolveSuperclasses()}
     */
    HashMap<String, Class> superclassesFound = new HashMap<>();

    /**
     * The references to classes used in associations found during the analysis
     * (to be later resolved by {@link #resolveAssociations()}
     */
    HashMap<String, ProxyAssociation> associationsFound = new HashMap<>();

    /**
     * As we will generate UML models, we use the Eclipse UML2 Factory
     */
    private UMLFactory umlFactory;

    /**
     * The resource set where the model will be stored. We keep it beacuse we have to
     * configure and customize some options
     */
    private ResourceSet resourceSet;

    /**
     * The model being created, the target.
     */
    private Model model;

    /**
     * We use this class as proxy when we cannot locate a referenced class
     */
    private Class unknown;

    /**
     * Primitive types to be used in the model
     */
    private HashMap<String, PrimitiveType> primitiveTypes = new HashMap<>();

    /**
     * Delegated constructor, it calls the {@link JSONSchemaToUML} constructor and uses the
     * value of {@link JSONSchemaToUML.DEFAULT_MODEL_NAME} as model name
     */
    public JSONSchemaToUML() {
    	initModel(DEFAULT_MODEL_NAME);
    }
    
    /**
     * Main constructor of the class. It basically initializes the model and the oracle
     * 
     * @param modelName The name for the model (and also the resulting file)
     */
    public JSONSchemaToUML(String modelName) {
        initModel(modelName);
    }
    
    /**
     * Returns the model being discovered
     * @return The model
     */
    public Model getModel() {
		return model;
	}

	/**
     * Launches the tool to traverse a file/folder with JSON schemas and generate the corresponding UML models
     * @param inputFile The file to analyze (it can be a file or a folder, if folder, it will be recursively traversed)
     */
    public void launch(File inputFile) {
        if(inputFile == null || !inputFile.exists())
            throw new JSONSchemaToUMLException("The file must exist");

        if(inputFile.isFile()) {
            analyze(inputFile);
        } else if(inputFile.isDirectory()) {
            for(File inFile: inputFile.listFiles()) {
                analyze(inFile);
            }
        } else
            throw new JSONSchemaToUMLException("Invalid input");

        resolveAssociations();
        resolveSuperclasses();
    }

    /**
     * Initializates the target model (i.e., gives a name, creates some annotations) and configures the resource set
     * to use the propor UML primitive types
     * @param modelName The name of the model
     */
    private void initModel(String modelName) {
        // Creating the model
        umlFactory = UMLFactory.eINSTANCE;
        model = umlFactory.createModel();
        model.setName(modelName);

        // Configurating the resource set
		resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		// Class to reuse when something goes wrong
        unknown = model.createOwnedClass("Unknown", false);
    }

    /**
     * Analyzes a file conforming to the JSON schema in order to create the corresponding UML elements (which will
     * be stored in both the oracle and the model)
     * 
     * @param file The file to analyze
     */
    private void analyze(File file) {
        // Let's start with the root element of the file
        JsonObject rootElement = null;
        try {
            JsonElement inputElement = (new JsonParser()).parse(new JsonReader(new FileReader(file)));
            rootElement = inputElement.getAsJsonObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Basic info from the schema
        if(!rootElement.has("id")) 
        	throw new JSONSchemaToUMLException("The root element MUST have an id");
        String id = rootElement.get("id").getAsString();
        JSONSchemaURI jsu = new JSONSchemaURI(id);
        // Inferring concept for this schema
        String modelConceptName = jsu.digestIdName();
        analyzeRootSchemaElement(modelConceptName, rootElement);
    }

    /**
     * Basic analyzer for JSON schema elements for which we already know that they are objects (or definitions)
     * and therefore will become concepts
     * @param name The name of the element
     * @param rootElement The JSON root element
     */
    private void analyzeRootSchemaElement(String name, JsonObject rootElement) {
        if(rootElement.has("type")) {
            String type = rootElement.get("type").getAsString();
            switch (type) {
                case "object":
                    analyzeObject(name, rootElement);
            }
        } else if(!rootElement.has("type") && rootElement.has("definitions")) {
            analyzeDefinitions(rootElement);
        } // TODO Cover other cases
    }

    /**
     * Analyzer for objects in the JSON schema. Objects are normally mapped into a corresponding UML class.
     * @param modelConceptName The name of the element
     * @param object The JSON object element
     */
    private Class analyzeObject(String modelConceptName, JsonObject object) {
        // Creating the concept
        Class concept = model.createOwnedClass(modelConceptName, false);

    	if(object.has("title")) {
            // 10.1 section in json-validation
    		String title = object.get("title").getAsString();
    		Comment comment = UMLFactory.eINSTANCE.createComment();
    		comment.setBody("Title: " + title);
    		concept.getOwnedComments().add(comment);
    	}
    	
    	if(object.has("description")) {
            // 10.1 section in json-validation
    		String title = object.get("description").getAsString();
    		Comment comment = UMLFactory.eINSTANCE.createComment();
    		comment.setBody("Description: " + title);
    		concept.getOwnedComments().add(comment);
    	}

        // Storing the concept
        oracle.put(modelConceptName, concept);

        if(object.has("allOf")) {
            // allOf represents a concept which has to successfully validate against all the schema elements
            // defined inside. We create an element which includes all the information described by allOf
            JsonArray allOfArray = object.get("allOf").getAsJsonArray();
            for(JsonElement allOfElement : allOfArray) {
                JsonObject allOfElementObj = allOfElement.getAsJsonObject();
                if(allOfElementObj.has("$ref")) {
                    // We interpret $ref elements as super classes for this element
                    // As such, the element should have been analyzed previously
                    String ref = allOfElementObj.get("$ref").getAsString();
                    String[] refSplit = ref.split("/");
                    String refClassName = refSplit[refSplit.length-1];
                    // We mark the concept to hav a super class, it will be resolved
                    // afterwards by the {@link #resolveSuperclasses()} method
                    superclassesFound.put(refClassName, concept);
                } else if(allOfElementObj.has("properties")) {
                    // Properties elements will become the attributes/references of the element
                    JsonObject propertiesObj = allOfElementObj.get("properties").getAsJsonObject();
                    for (Entry<String, JsonElement> entry : propertiesObj.entrySet()) {
                    	String propertyKey = entry.getKey();
                        JsonObject propertyObj = propertiesObj.get(propertyKey).getAsJsonObject();
                        analyzeProperty(concept, propertyKey, propertyObj);
                    }
                } // TODO Cover more cases according to the specificiation
            }
        } else if (object.has("properties")) {
            // When an element has directly "properties" may mean that it does not have superclasses
            // It is also used in definitions
            JsonObject propertiesObj = object.get("properties").getAsJsonObject(); 
            for (Entry<String, JsonElement> entry : propertiesObj.entrySet()) {
            	String propertyKey = entry.getKey();
                JsonObject propertyObj = propertiesObj.get(propertyKey).getAsJsonObject();
                analyzeProperty(concept, propertyKey, propertyObj);
            }
        }
        if (object.has("required")) {
            // 6.5.3 section in json-validation
            // This constraint specifies the set of properties that have to be there (e.g., the min
            // cardinality is 1
            for(JsonElement reqElem : object.get("required").getAsJsonArray()) {
                String reqElemString = reqElem.getAsString();
                for(Property property : concept.getOwnedAttributes()) {
                    if(property.getName().equals(reqElemString)) {
                        property.setLower(1);
                        break;
                    }
                }
            }
        }

        return concept;
    }

    /**
     * Analyzes a property for an object/concept
     * @param concept The concept which includes such property
     * @param propertyName The name of the property
     * @param object The JSON object element to analyze
     */
    private void analyzeProperty(Class concept, String propertyName, JsonObject object) {
        if(object.has("type")) {
            Type modelAttType = null;
            String propertyObjType = object.get("type").getAsString();
            if(object.has("enum")) {
                // Section 6.1.2. We create an enumeration
                JsonArray enumValues = object.get("enum").getAsJsonArray();
                Enumeration enumeration = model.createOwnedEnumeration(propertyName+"Enum");
                for(JsonElement enumValueElem : enumValues) {
                    String enumValue = enumValueElem.getAsString();
                    enumeration.getOwnedLiterals().add(enumeration.createOwnedLiteral(enumValue));
                }
                concept.createOwnedAttribute(propertyName, enumeration);
            } else if (propertyObjType.equals("string")) {
                if(object.has("format")) {
                    String propertyFormat = object.get("format").getAsString();
                    if(propertyFormat.equals("date-time")) {
                        modelAttType = getPrimitiveType("Date");
                    }
                } else {
                    modelAttType = getPrimitiveType("String");
                }
                if(object.has("maxLength"))
                    // Section 6.3.1 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "maxLengthConstraint",
                            "self." + propertyName + ".size() <= " + object.get("maxLength").getAsString());
                if(object.has("minLength"))
                    // Section 6.3.2 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "minLengthConstraint",
                            "self." + propertyName + ".size() >= " + object.get("minLength").getAsString());
                if(object.has("pattern")) {
                    // Section 6.3.3 in json-schema-validation. Resolved as OCL
                    // TODO 6.3.3 in json-schema-validation
                }
                concept.createOwnedAttribute(propertyName, modelAttType);
            } else if(propertyObjType.equals("integer") || propertyObjType.equals("number")) {
                modelAttType = getPrimitiveType("Integer");
                concept.createOwnedAttribute(propertyName, modelAttType);
                if(object.has("multipleOf"))
                    // Section 6.2.1 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "multipleOfConstraint",
                            "self." + propertyName + ".div("+object.get("multipleOf").getAsString()+") = 0");
                if(object.has("maximum"))
                    // Section 6.2.2 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "maximumConstraint",
                            "self." + propertyName + " <= " + object.get("maximum").getAsString());
                if(object.has("exclusiveMaximum"))
                    // Section 6.2.3 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "exclusiveMaximumConstraint",
                            "self." + propertyName + " < " + object.get("exclusiveMaximum").getAsString());
                if(object.has("minimum"))
                    // Section 6.2.4 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "minimumConstraint",
                            "self." + propertyName + " >= " + object.get("minimum").getAsString());
                if(object.has("exclusiveMinimum"))
                    // Section 6.2.5 in json-schema-validation. Resolved as OCL
                    addConstraint(concept, propertyName, "exclusiveMinimumConstraint",
                            "self." + propertyName + " > " + object.get("exclusiveMinimum").getAsString());

            } else if(propertyObjType.equals("boolean")) {
                concept.createOwnedAttribute(propertyName, getPrimitiveType("Boolean"));
            } else if(propertyObjType.equals("array")) {

            } else if (propertyObjType.equals("object")) {
                String toCamelCase = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1, propertyName.length());
                Class target = analyzeObject(toCamelCase, object);
                concept.createAssociation(true, AggregationKind.NONE_LITERAL, propertyName, 0, -1, target, false, AggregationKind.NONE_LITERAL, concept.getName(), 1, 1);
                if(object.has("maxProperties")) {

                }
            } // TODO Cover more types
        } else if(object.has("$ref")) {
            String ref = object.get("$ref").getAsString();
            String[] refSplit = ref.split("/");
            String refClassName = refSplit[refSplit.length-1];
            ProxyAssociation proxy = new ProxyAssociation();
            proxy.sourceComposition = true; proxy.targetComposition = false;
            proxy.sourceKind = AggregationKind.NONE_LITERAL; proxy.targetKind = AggregationKind.NONE_LITERAL;
            proxy.sourceEnd = propertyName; proxy.targetEnd = refClassName;
            proxy.sourceLower = 0; proxy.targetLower = 1;
            proxy.sourceUpper = -1; proxy.targetUpper = 1;
            proxy.owner = concept;
            associationsFound.put(refClassName, proxy);
        } else if(object.has("oneOf")) {
            // TODO check this case
        } // TODO cover more cases according to the specification
    }

    /**
     * Adds a OCL constraint to a concept
     * @param concept The concept which holds the constraint
     * @param constraintName The name of the constraint (will be eventually formed
     *                       as conceptName-constraintName-constraintType
     * @param constraintType The type of the constraint being applied (e.g., macLengthConstraint)
     * @param constraintExp The OCL expression
     */
    private void addConstraint(Class concept, String constraintName, String constraintType, String constraintExp) {
        Constraint constraint = UMLFactory.eINSTANCE.createConstraint();
        String constraintId= concept.getName()+"-"+constraintName+"-"+constraintType;
        constraint.setName(constraintId);
        OpaqueExpression expression = UMLFactory.eINSTANCE.createOpaqueExpression();
        expression.getLanguages().add("OCL");
        expression.getBodies().add(constraintExp);
        constraint.setSpecification(expression);
        concept.getOwnedRules().add(constraint);
    }

    /**
     * Query the oracle to get a previously created class given a name
     * @param refClassName The name to look up
     * @return The found class (null if nothing)
     */
    private Class queryOracle(String refClassName) {
        if(oracle.containsKey(refClassName)) {
            return oracle.get(refClassName);
        } else
            return null;
    }

    /**
     * Resolve the associations of the classes. The analysis process includes proxies to be resolved
     * afterwards. They are resolved by this method :)
     */
    private void resolveAssociations() {
        for(String refClassName : associationsFound.keySet()) {
            ProxyAssociation proxy = associationsFound.get(refClassName);
            Class owner = proxy.owner;
            Class foundClass = queryOracle(refClassName);
            if(foundClass == null) {
                foundClass = unknown;
            }
            owner.createAssociation(proxy.sourceComposition, proxy.sourceKind, proxy.sourceEnd, proxy.sourceLower, proxy.sourceUpper, foundClass, proxy.targetComposition, proxy.targetKind, proxy.targetEnd, proxy.targetLower, proxy.targetUpper);
        }
    }

    /**
     * Resolve superclasses. The analysis process registers the superclasses to be resolved afterward.
     * They are resolved by this method :)
     */
    private void resolveSuperclasses() {
        for(String refClassName : superclassesFound.keySet()) {
            Class subClass = superclassesFound.get(refClassName);
            Class foundClass = queryOracle(refClassName);
            if(foundClass == null) {
                foundClass = unknown;
            }
            subClass.getSuperClasses().add(foundClass);
        }
    }

    /**
     * Definition are usually created to be reused among the different JSON schemas.
     * @param object The JSON object including the definitions
     */
    private void analyzeDefinitions(JsonObject object) {
        JsonObject definitionsObj = object.get("definitions").getAsJsonObject();
        for(Entry<String, JsonElement> entry : definitionsObj.entrySet()) {
        	String definitionKey = entry.getKey();
            JsonObject definitionObj = definitionsObj.get(definitionKey).getAsJsonObject();
            analyzeRootSchemaElement(definitionKey, definitionObj);
        }
    }

    /**
     * Saves the model. It uses the resource set configured previously, as it includes some options to properly
     * resolve pathmaps and so on.
     */
    public void saveModel(File target) {
        Resource resource = resourceSet.createResource(URI.createFileURI(target.getAbsolutePath())); // TODO Configure the name
        resource.getContents().add(model);
        try {
            resource.save(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Saves the model given an URI
     * 
     * @param target the target URI
     */
    public void saveModel(URI target) {
        Resource resource = resourceSet.createResource(target);
        resource.getContents().add(model);
        try {
            resource.save(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns (or create) the UML primitive type for a given string-based name.
     * Primitive types are created on demand.
     * 
     * @param commonName The string-based name of the type
     * @param model The model element
     * @return The primitive type
     */
	private PrimitiveType getPrimitiveType(String typeName) {
		PrimitiveType found = primitiveTypes.get(typeName);
		if(found == null) {
			found = umlFactory.createPrimitiveType();
			found.setName(typeName);
			model.getOwnedTypes().add(found);
			primitiveTypes.put(typeName, found);
			
		} 
		return found;
	}
}
