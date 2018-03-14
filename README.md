# jsonSchema-to-uml

A tool to generate UML models from JSON schema documents

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
https://github.com/SOM-Research/jsonschema-to-uml/update
```
4. Select *JSONSchema to UML* then click on *Next*.
5. Follow the the rest of the steps (license, etc...) and reboot Eclipse.

## Using the plugin

1. Create a Project or use an existing project in your workspace.
2. Import the JSON Schema documents.
3. Right-click on the definition file and select *JSONSchema to UML/Generate a Class diagram*. This will generate the UML model corresponding to the input definition under the folder *src-gen* of your project.

## Visualizing the Class diagram using Papyrus

1. Install Papyrus if you didn't do it yet (You can find the instructions [here](https://www.eclipse.org/papyrus/download.html)).
2. Open the perspective *Papyrus*.
3. Right-click on the generated UML model and select *New -> Papyrus Model*.
4. Follow the steps in the wizard to initialize a Class diagram (keep everything as predefined except in the *Initialization information* step where you should check *Class Diagram* as the Representation kind).
5. Drag-and-drop the UML elements from the *Model Explrer* into the editor.
6. Align and arrange the layout as you prefer.
7. Save.