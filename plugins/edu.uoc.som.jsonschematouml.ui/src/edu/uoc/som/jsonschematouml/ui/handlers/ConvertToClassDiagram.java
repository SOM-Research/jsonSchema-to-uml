package edu.uoc.som.jsonschematouml.ui.handlers;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.uoc.som.jsonschematouml.generators.JSONSchemaToUML;
import edu.uoc.som.jsonschematouml.ui.JSONSchemaToUMLUIPlugin;

/**
 * Main handler class to respond to the main contribution to Eclipse UI. This handler
 * executes the JSON Schema to UML tool for the set of files selected by the user
 *
 */
public class ConvertToClassDiagram extends AbstractHandler {

	/**
	 * ID for this plugin
	 */
	public static final String ID = "edu.uoc.som.jsonschematouml.ui.popup.handlers.ConvertToClassDiagram";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		if (selection != null & selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Job job = new Job(ID) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					if (monitor == null) monitor = new NullProgressMonitor();
					try {
						monitor.beginTask("Generating the Class diagram", IProgressMonitor.UNKNOWN);
						for (Iterator<?> iterator = structuredSelection.iterator(); iterator.hasNext();) {
							Object obj = iterator.next();
							if (obj instanceof IFile) {
								IFile iFile = (IFile) obj;
								IContainer target = iFile.getProject().getFolder("src-gen");
								if (!target.getLocation().toFile().exists()) {
									target.getLocation().toFile().mkdirs();	
									iFile.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
								}
								JSONSchemaToUML generator = new JSONSchemaToUML("test");
								generator.launch(new File(iFile.getLocation().toString()));
								
								String location = target.getFullPath().toString();
								String fileName = iFile.getName().substring(0, iFile.getName().lastIndexOf('.'));
						        URI finalLocation = URI.createPlatformResourceURI(location, true).appendSegment(fileName).appendFileExtension("uml");
						        System.out.println(finalLocation.toString());
								generator.saveModel(finalLocation);
								
							}
						}
					} catch (CoreException e) {
						return new Status(IStatus.ERROR, JSONSchemaToUMLUIPlugin.PLUGIN_ID, e.getLocalizedMessage(), e);
						
					} finally {
						monitor.done();
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
			
		}
		return null;
	}
}
