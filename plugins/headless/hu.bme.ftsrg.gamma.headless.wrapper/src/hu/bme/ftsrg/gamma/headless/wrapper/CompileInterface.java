package hu.bme.ftsrg.gamma.headless.wrapper;
/*
import java.io.File;
import java.io.IOException;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.eclipse.core.commands.ExecutionException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import org.yakindu.sct.model.sgraph.Statechart;

import hu.bme.mit.gamma.dialog.DialogUtil;
import hu.bme.mit.gamma.statechart.language.ui.serializer.StatechartLanguageSerializer;
import hu.bme.mit.gamma.statechart.interface_.Package;
import hu.bme.mit.gamma.yakindu.transformation.batch.InterfaceTransformer;
import hu.bme.mit.gamma.yakindu.transformation.traceability.Y2GTrace;
*/
public class CompileInterface {
/*
	protected Logger logger = Logger.getLogger("GammaLogger");

	public Object execute() throws ExecutionException {
		try {
		
			String fullPath = "/hu.bme.mit.gamma.tutorial.start/model/Interfaces/Interfaces.sct";
			String locationURI = "file:C:\\Eclipse-2020-09-workspace\\hu.bme.mit.gamma.tutorial.start\\model\\Interfaces\\Interfaces.sct";
			ResourceSet resSet = new ResourceSetImpl();
			logger.log(Level.INFO, "Resource set for Yakindu to Gamma interface generation: " + resSet);
			URI fileURI = URI.createPlatformResourceURI(fullPath, true);
			Resource resource;
			try {
				resource = resSet.getResource(fileURI, true);
			} catch (RuntimeException e) {
				return null;
			}
			if (resource.getContents() != null) {
				if (resource.getContents().get(0) instanceof Statechart) {
					Statechart statechart = (Statechart) resource.getContents().get(0);
					if (!statechart.getRegions().isEmpty()) {
						logger.log(Level.INFO, "This statechart contains regions, and not just a single interface!");
					}
					String fileURISubstring = locationURI.substring(5);
					String parentFolder = fileURISubstring.substring(0, fileURISubstring.lastIndexOf("/"));
					// No file extension
					String fileName = fileURISubstring.substring(fileURISubstring.lastIndexOf("/") + 1,
							fileURISubstring.lastIndexOf("."));
					logger.log(Level.INFO, "Resource set content for Yakindu to Gamma interface generation: " + resSet);
					SimpleEntry<Package, Y2GTrace> resultModels = new InterfaceTransformer(statechart,
							statechart.getName()).execute();
					saveModel(resultModels.getKey(), parentFolder, fileName + ".gcd");
					saveModel(resultModels.getValue(), parentFolder, "." + fileName + ".y2g");
					logger.log(Level.INFO, "The Yakindu-Gamma interface transformation has been finished.");
				}
			}
			return null;
		} catch (Exception exception) {
			exception.printStackTrace();
			logger.log(Level.SEVERE, exception.getMessage());
			DialogUtil.showErrorWithStackTrace(exception.getMessage(), exception);
		}
		return null;
	}

	/**
	 * Responsible for saving the given element into a resource file.
	 *//*
	private void saveModel(EObject rootElem, String parentFolder, String fileName) throws IOException {
		if (rootElem instanceof Package) {
			try {
				// Trying to serialize the model
				serialize(rootElem, parentFolder, fileName);
			} catch (Exception e) {
				e.printStackTrace();
				logger.log(Level.WARNING, e.getMessage() + System.lineSeparator()
						+ "Possibly you have two more model elements with the same name specified in the previous error message.");
				new File(parentFolder + File.separator + fileName).delete();
				// Saving like an EMF model
				String newFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".gsm";
				normalSave(rootElem, parentFolder, newFileName);
			}
		} else {
			// It is not a statechart model, regular saving
			normalSave(rootElem, parentFolder, fileName);
		}
	}

	private void normalSave(EObject rootElem, String parentFolder, String fileName) throws IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource saveResource = resourceSet
				.createResource(URI.createFileURI(URI.decode(parentFolder + File.separator + fileName)));
		saveResource.getContents().add(rootElem);
		saveResource.save(Collections.EMPTY_MAP);
	}

	private void serialize(EObject rootElem, String parentFolder, String fileName) throws IOException {
		StatechartLanguageSerializer serializer = new StatechartLanguageSerializer();
		serializer.serialize(rootElem, parentFolder, fileName);
	}
*/
}