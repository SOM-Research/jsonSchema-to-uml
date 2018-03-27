

# jsonSchema-to-uml

A tool to generate UML models from JSON schema documents.

## Requirements

To generate UML models:

- Eclipse Modeling tools (it can be found [here](http://www.eclipse.org/downloads/packages/eclipse-modeling-tools/oxygen2)).

To visualize the generated UML models:

- A UML 2.5 modeling environment in Eclipse such as [Papyrus](https://www.eclipse.org/papyrus/) or [UMLDesigner](https://marketplace.eclipse.org/content/uml-designer) (we tested the tool with Papyrus).

## Installation

1. Open Eclipse IDE
2. Click on *Help / Install New Software...*
3. Click on *Add...* and fill in the form as indicated. The update site is 
```
https://som-research.github.io/jsonSchema-to-uml/update/
```
4. Select *JSONSchema to UML* then click on *Next*.
5. Follow the the rest of the steps (license, etc...) and reboot Eclipse.

## Using the plugin

1. Create a Project or use an existing project in your workspace.
2. Import the JSON Schema documents.
3. To generate a UML model from your documents you can right-click on a file or a folder containing your documents.
4. A UML model corresponding to the input definition will be generated in the folder *src-gen* of your project.

## Visualizing the Class diagram using Papyrus

1. Install Papyrus if you didn't do it yet (You can find the instructions [here](https://www.eclipse.org/papyrus/download.html)).
2. Open the perspective *Papyrus*.
3. Right-click on the generated UML model and select *New -> Papyrus Model*.
4. Follow the steps in the wizard to initialize a Class diagram (keep everything as predefined except in the *Initialization information* step where you should check *Class Diagram* as the Representation kind).
5. Drag-and-drop the UML elements from the *Model Explrer* into the editor.
6. Align and arrange the layout as you prefer.
7. Save.

## The mapping in a nutshell
The generation process apply this (non exhaustive) list of mappings:
* Each JSON Schema element is represented by a UML Class
* Properties in JSON Schema elements represent the properties of a UML Class. 
  * If the property is of primitive type it will become an attribute in the UML Class
  * If the property is of type enum, an Enumeration will be created and an attribute in the UML Class will be added
  * If the property is of type object or refers to other element (using $ref), an association is created in the UML Class. The type of the association corresponds to the UML Class element created from the object (or the referred object)
* Hierarchies are created from ``allOf``, ``oneOf``, ``anyOf``
* The elements defined in ``definitions`` are considered as a library of JSON Schema elements and therefore they generate new UML Classes
* The folder structure is used to created UML Packages containing the UML Classes coming from the JSON Schema files.