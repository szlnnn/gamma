package hu.bme.ftsrg.gamma.headless.wrapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import hu.bme.mit.gamma.codegenerator.java.GlueCodeGenerator;
import hu.bme.mit.gamma.dialog.DialogUtil;
import hu.bme.mit.gamma.statechart.interface_.Component;
import hu.bme.mit.gamma.statechart.interface_.Package;
import hu.bme.mit.gamma.statechart.language.*;
import hu.bme.mit.gamma.statechart.statechart.StatechartDefinition;
import hu.bme.mit.gamma.yakindu.transformation.traceability.Y2GTrace;

public class CodeGenerator {
protected Logger logger = Logger.getLogger("GammaLogger");
	
	protected final String folderName = "src-gen";
	
	public Object execute() throws CoreException {
			String filePath = "/model/Crossroad.gcd";
			IPath path = new Path(filePath);
		 IProject p =
		    		 ResourcesPlugin.getWorkspace().getRoot().getProject("hu.bme.mit.gamma.tutorial.finish");
		    IFile file = (IFile)p.findMember(filePath);
			Injector injector = new StatechartLanguageStandaloneSetupGenerated().createInjectorAndDoEMFRegistration();
			XtextResourceSet resSet = injector.getInstance(XtextResourceSet.class);
						//ResourceSet resSet = new ResourceSetImpl();
						logger.log(Level.INFO, "Resource set for Java code generation created: " + resSet);
						
						URI compositeSystemURI = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
						// Loading the composite system to the resource set
						Resource resource = loadResource(resSet, compositeSystemURI);
						Package compositeSystem = (Package) resource.getContents().get(0);
						// Checking whether all the statecharts have unique names
						checkStatechartNameUniqueness(file.getProject(), new HashSet<String>());
						// Getting the simple statechart names
						Collection<String> simpleStatechartFileNames = getSimpleStatechartFileNames(compositeSystem);
						// Very important step: recursively attaining the Yakindu-Gamma traces from the project folders based on the imported Statecharts of the composite system
						List<URI> uriList = new ArrayList<URI>();
						obtainTraceURIs(file.getProject(), simpleStatechartFileNames, uriList);
						if (simpleStatechartFileNames.size() != uriList.size()) {
							logger.log(Level.INFO, "Some trace model is not found: " +
									simpleStatechartFileNames + System.lineSeparator() + uriList + System.lineSeparator() +
									"Wrapper is not generated for the Gamma statecharts without trace.");
						}
						for (URI uri : uriList) {
							loadResource(resSet, uri);
						}
						// Setting the URI so the composite system code will be generated into a separate package
						String parentFolder = file.getProject().getLocation() + "/" + folderName;
						// Decoding so spaces do not stir trouble
						parentFolder = URI.decode(parentFolder);
						logger.log(Level.INFO, "Resource set content for Java code generation: " + resSet);
						String packageName = file.getProject().getName().toLowerCase();
						GlueCodeGenerator generator = new GlueCodeGenerator(resSet, packageName, parentFolder);
						generator.execute();
						generator.dispose();
						logger.log(Level.INFO, "The Java code generation has been finished.");
						return null;
	}
	
	void processContainer(IContainer container) throws CoreException
	{
	   IResource [] members = container.members();

	   for (IResource member : members)
	    {
	      if (member instanceof IContainer) 
	       {
	         processContainer((IContainer)member);
	       }
	      else if (member instanceof IFile)
	       {
	         System.out.println(member.getFullPath().toString());
	       }
	    }
	}
	
	/**
	 * Checks whether all statecharts have unique names.
	 */
	protected void checkStatechartNameUniqueness(IContainer container, Set<String> fileNames) throws CoreException {
		for (IResource iResource : container.members()) {
			if (iResource instanceof IFile) {
				IFile file = (IFile) iResource;
				String fileName = file.getName();
				// It is faster to check only Gamma statecharts: gsm and gcd file extensions
				if (fileName.endsWith(".gsm") || fileName.endsWith(".gcd") ) {
					if (fileNames.contains(fileName)) {
						throw new IllegalArgumentException("Multiple statechart files with the same name: " + fileName
							+ ". Please use different names for the statecharts!");
					}
					fileNames.add(fileName);
				}
			}
			else if (iResource instanceof IContainer) {
				checkStatechartNameUniqueness((IContainer) iResource, fileNames);
			}
		}
	}
	
	/**
	 * Returns the names of the imported statecharts recursively starting from the given project.
	 */
	protected Collection<String> getSimpleStatechartFileNames(Package gammaPackage) {
		Set<String> simpleStatechartNameList = new HashSet<String>();
		for (Package importedPackage : gammaPackage.getImports()) {
			if (hasOnlyStatecharts(importedPackage)) {
				// Adding the name of the file of the statechart to the list
				String packageFileName = getPackageFileName(importedPackage);
				simpleStatechartNameList.add(packageFileName);
			}
			else {				
				// Recursively doing this with referred composite systems
				final Collection<String> names = getSimpleStatechartFileNames(importedPackage);
				simpleStatechartNameList.addAll(names);
			}
		}
		return simpleStatechartNameList;
	}
	
	protected boolean hasOnlyStatecharts(Package gammaPackage) {
		// Only statecharts (theoretically single statechart) are contained
		Collection<Component> components = gammaPackage.getComponents();
		return !components.isEmpty() && components.stream().allMatch(it -> it instanceof StatechartDefinition);
	}
	
	protected String getPackageFileName(Package _package) {
		URI uri = _package.eResource().getURI();
		// /hu.bme.mit.gamma.tutorial.extra/model/Monitor/Monitor.gcd
		String packageFileName = uri.lastSegment();
		// Monitor.gcd -> ["Monitor", "gcd"]
		String[] splittedPackageFileName = packageFileName.split("\\.");
		return splittedPackageFileName[0];
	}
	
	/**
	 * Puts the URIs of the Yakindu-Gamma trace files into the URIList if the trace file has a name contained in importList.
	 */
	protected void obtainTraceURIs(IContainer container, Collection<String> importList, List<URI> URIList) throws CoreException {
		for (IResource iResource : container.members()) {
			if (iResource instanceof IFile) {
				IFile file = (IFile) iResource;
				String[] fileName = file.getName().split("\\.");
				// Starts with index 1, because the traces are hidden files so their names start with a '.'
				if (fileName.length >= 3 && importList.contains(fileName[1]) && fileName[2].equals("y2g")) {
					URIList.add(URI.createPlatformResourceURI(file.getFullPath().toString(), true));
				}
			}
			else if (iResource instanceof IContainer) {
				obtainTraceURIs((IContainer) iResource, importList, URIList);
			}
		}
	}

	protected Resource loadResource(ResourceSet resSet, URI uri) throws IllegalArgumentException {
		Resource resource = resSet.getResource(uri, true);
		EObject object = resource.getContents().get(0);
		if (!(object instanceof Package || object instanceof Y2GTrace)) {
			throw new IllegalArgumentException("There can be only Packages and Traces in the selection: " + resource.getContents().get(0));
		}
		return resource;
	}
}
