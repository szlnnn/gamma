/********************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Gamma project
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package hu.bme.mit.gamma.api;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;
import hu.bme.mit.gamma.api.taskhandler.AdaptiveContractTestGenerationHandler;
import hu.bme.mit.gamma.api.taskhandler.AnalysisModelTransformationHandler;
import hu.bme.mit.gamma.api.taskhandler.CodeGenerationHandler;
import hu.bme.mit.gamma.api.taskhandler.EventPriorityTransformationHandler;
import hu.bme.mit.gamma.api.taskhandler.PhaseGenerationHandler;
import hu.bme.mit.gamma.api.taskhandler.SlicingHandler;
import hu.bme.mit.gamma.api.taskhandler.StatechartCompilationHandler;
import hu.bme.mit.gamma.api.taskhandler.TestGenerationHandler;
import hu.bme.mit.gamma.api.taskhandler.TestReplayModelGenerationHandler;
import hu.bme.mit.gamma.api.taskhandler.VerificationHandler;
import hu.bme.mit.gamma.genmodel.derivedfeatures.GenmodelDerivedFeatures;
import hu.bme.mit.gamma.genmodel.language.GenModelStandaloneSetupGenerated;
import hu.bme.mit.gamma.genmodel.model.AdaptiveContractTestGeneration;
import hu.bme.mit.gamma.genmodel.model.AnalysisModelTransformation;
import hu.bme.mit.gamma.genmodel.model.CodeGeneration;
import hu.bme.mit.gamma.genmodel.model.EventPriorityTransformation;
import hu.bme.mit.gamma.genmodel.model.GenModel;
import hu.bme.mit.gamma.genmodel.model.InterfaceCompilation;
import hu.bme.mit.gamma.genmodel.model.PhaseStatechartGeneration;
import hu.bme.mit.gamma.genmodel.model.Slicing;
import hu.bme.mit.gamma.genmodel.model.StatechartCompilation;
import hu.bme.mit.gamma.genmodel.model.Task;
import hu.bme.mit.gamma.genmodel.model.TestGeneration;
import hu.bme.mit.gamma.genmodel.model.TestReplayModelGeneration;
import hu.bme.mit.gamma.genmodel.model.Verification;
import hu.bme.mit.gamma.genmodel.model.YakinduCompilation;
import hu.bme.mit.gamma.property.language.PropertyLanguageStandaloneSetupGenerated;
import hu.bme.mit.gamma.statechart.language.StatechartLanguageStandaloneSetupGenerated;
import hu.bme.mit.gamma.trace.language.TraceLanguageStandaloneSetupGenerated;
import hu.bme.mit.gamma.api.taskhandler.InterfaceCompilationHandler;

public class GammaApi {
	
	protected Logger logger = Logger.getLogger("GammaLogger");

	/**
	 * Executes the Gamma tasks based on the ggen model specified by the fullPath parameter,
	 *  e.g., /hu.bme.mit.gamma.tutorial.start/model/Controller/Controller.ggen.
	 * @param fileWorkspaceRelativePath IFile.fullPath method of the file containing the ggen model
	 * @throws CoreException 
	 * @throws IOException 
	 */
	public void run(String fileWorkspaceRelativePath) throws Exception {
		URI fileURI = URI.createPlatformResourceURI(fileWorkspaceRelativePath, true);
		// Eclipse magic: URI -> IFile
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = workspaceRoot.getFile(new Path(fileURI.toPlatformString(true)));
		IProject project = file.getProject();
		// Xtext errors can be removed by cleaning the project in case of YakinduCompilation and TestGeneration tasks
		boolean needsCleaning = false;
		Injector injector = new StatechartLanguageStandaloneSetupGenerated().createInjectorAndDoEMFRegistration();
		
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		// Multiple compilations due to the dependencies between models
		final int MAX_ITERATION_COUNT = 6;
		for (int i = 0; i < MAX_ITERATION_COUNT; ++i) {
	
			Resource resource = resourceSet.getResource(fileURI, true);
			// Assume that the resource has a single object as content
			EObject content = resource.getContents().get(0);
			if (content instanceof GenModel) {
				GenModel genmodel = (GenModel) content;
				// WARNING: workspace location and imported project locations are not to be confused
				// Sorting: InterfaceCompilation < StatechartCompilation < else does not work as the generated models are not reloaded
				List<Task> tasks = orderTasks(genmodel, i);
				for (Task task : tasks) {
					if (task instanceof YakinduCompilation) {
						needsCleaning = true;
						if (task instanceof InterfaceCompilation) {
							logger.log(Level.INFO, "Resource set content for Yakindu to Gamma interface generation: " + resourceSet);
							InterfaceCompilation interfaceCompilation = (InterfaceCompilation) task;
							InterfaceCompilationHandler handler = new InterfaceCompilationHandler(file);
							handler.setTargetFolder(interfaceCompilation);
							handler.execute(interfaceCompilation);
							logger.log(Level.INFO, "The Yakindu-Gamma interface transformation has been finished.");
						}
						else if (task instanceof StatechartCompilation) {
							logger.log(Level.INFO, "Resource set content Yakindu to Gamma statechart generation: " + resourceSet);
							StatechartCompilation statechartCompilation = (StatechartCompilation) task;
							StatechartCompilationHandler handler = new StatechartCompilationHandler(file);
							handler.setTargetFolder(statechartCompilation);
							handler.execute(statechartCompilation);
							logger.log(Level.INFO, "The Yakindu-Gamma transformation has been finished.");
						}
					} else {
						final String projectName = project.getName().toLowerCase();
						if (task instanceof CodeGeneration) {
							CodeGeneration codeGeneration = (CodeGeneration) task;
							logger.log(Level.INFO, "Resource set content for Java code generation: " + resourceSet);
							CodeGenerationHandler handler = new CodeGenerationHandler(file);
							handler.setTargetFolder(codeGeneration);
							handler.execute(codeGeneration, projectName);
							logger.log(Level.INFO, "The Java code generation has been finished.");
						}
						else if (task instanceof AnalysisModelTransformation) {
							AnalysisModelTransformation analysisModelTransformation = (AnalysisModelTransformation) task;
							AnalysisModelTransformationHandler handler = new AnalysisModelTransformationHandler(file);
							handler.setTargetFolder(analysisModelTransformation);
							handler.execute(analysisModelTransformation); 
							logger.log(Level.INFO, "FUNCTIONALITY NOT SUPPORTED IN HEADLESS");
						}
						else if (task instanceof TestGeneration) {
							needsCleaning = true;
							TestGeneration testGeneration = (TestGeneration) task;
							TestGenerationHandler handler = new TestGenerationHandler(file);
							handler.setTargetFolder(testGeneration);
							handler.execute(testGeneration, projectName);
							logger.log(Level.INFO, "The test generation has been finished.");
						}
						else if (task instanceof Verification) {
							needsCleaning = true;
							Verification verification = (Verification) task;
							VerificationHandler handler = new VerificationHandler(file);
							handler.setTargetFolder(verification);
							handler.execute(verification);
							logger.log(Level.INFO, "The verification has been finished.");
						}
						else if (task instanceof Slicing) {
							Slicing slicing = (Slicing) task;
							SlicingHandler handler = new SlicingHandler(file);
							// No target folder setting, that is done inside
							handler.execute(slicing);
							logger.log(Level.INFO, "The slicing has been finished.");
						}
						else if (task instanceof TestReplayModelGeneration) {
							TestReplayModelGeneration testReplayModelGeneration = (TestReplayModelGeneration) task;
							TestReplayModelGenerationHandler handler = new TestReplayModelGenerationHandler(file);
							handler.setTargetFolder(testReplayModelGeneration);
							handler.execute(testReplayModelGeneration);
							logger.log(Level.INFO, "The test replay model generation has been finished.");
						}
						else if (task instanceof AdaptiveContractTestGeneration) {
							AdaptiveContractTestGeneration testGeneration = (AdaptiveContractTestGeneration) task;
							AdaptiveContractTestGenerationHandler handler = new AdaptiveContractTestGenerationHandler(file);
							handler.setTargetFolder(testGeneration);
							handler.execute(testGeneration, file.getLocation().toString(), projectName);
							logger.log(Level.INFO, "The adaptive contract test generation has been finished.");
						}
						else if (task instanceof EventPriorityTransformation) {
							needsCleaning = true;
							EventPriorityTransformation eventPriorityTransformation = (EventPriorityTransformation) task;
							EventPriorityTransformationHandler handler = new EventPriorityTransformationHandler(file);
							handler.setTargetFolder(eventPriorityTransformation);
							handler.execute(eventPriorityTransformation);
							logger.log(Level.INFO, "The event priority transformation has been finished.");
						}
						else if (task instanceof PhaseStatechartGeneration) {
							needsCleaning = true;
							PhaseStatechartGeneration phaseStatechartGeneration = (PhaseStatechartGeneration) task;
							PhaseGenerationHandler handler = new PhaseGenerationHandler(file);
							handler.setTargetFolder(phaseStatechartGeneration);
							handler.execute(phaseStatechartGeneration);
							logger.log(Level.INFO, "The phase statechart transformation has been finished.");
						}
					}
				}
			}
			else {
				logger.log(Level.WARNING, "The given resource does not contain a GenModel: " + resource);
			}
		}
		if (needsCleaning) {
			logger.log(Level.INFO, "Cleaning project...");
			// To reload imports
			project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
			logger.log(Level.INFO, "Cleaning project finished.");
		}
	}	
	
	/** 
	 * Compilation order: interfaces <- statecharts <- event priority <- analysis model, code <- test.
	 * As everything depends on statecharts and statecharts depend on interfaces.
	 * This way the user does not have to compile two or three times.
	 */
	private List<Task> orderTasks(GenModel genmodel, int iteration) {
		List<Task> allTasks = GenmodelDerivedFeatures.getAllTasks(genmodel);
		switch (iteration) {
			case 0: 
				return allTasks.stream()
						.filter(it -> it instanceof InterfaceCompilation)
						.collect(Collectors.toList());
			case 1: 
				return allTasks.stream()
						.filter(it -> it instanceof StatechartCompilation)
						.collect(Collectors.toList());
			case 2: 
				return allTasks.stream()
						.filter(it -> it instanceof EventPriorityTransformation ||
								it instanceof PhaseStatechartGeneration)
						.collect(Collectors.toList());
			case 3: 
				return allTasks.stream()
						.filter(it -> it instanceof AnalysisModelTransformation ||
								it instanceof CodeGeneration)
						.collect(Collectors.toList());
			case 4: 
				return allTasks.stream()
						.filter(it -> it instanceof Slicing)
						.collect(Collectors.toList());
			case 5: 
				return allTasks.stream()
						.filter(it -> it instanceof TestGeneration || it instanceof Verification ||
								it instanceof AdaptiveContractTestGeneration ||
								it instanceof TestReplayModelGeneration)
						.collect(Collectors.toList());
			default: 
				throw new IllegalArgumentException("Not known iteration variable: " + iteration);
		}
	}
	
}