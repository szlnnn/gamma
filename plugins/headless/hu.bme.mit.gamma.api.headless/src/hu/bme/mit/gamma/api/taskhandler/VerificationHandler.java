/********************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Gamma project
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package hu.bme.mit.gamma.api.taskhandler;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

import hu.bme.mit.gamma.genmodel.model.AnalysisLanguage;
import hu.bme.mit.gamma.genmodel.model.Verification;
import hu.bme.mit.gamma.property.model.CommentableStateFormula;
import hu.bme.mit.gamma.property.model.PropertyPackage;
import hu.bme.mit.gamma.querygenerator.serializer.PropertySerializer;
import hu.bme.mit.gamma.querygenerator.serializer.ThetaPropertySerializer;
import hu.bme.mit.gamma.querygenerator.serializer.UppaalPropertySerializer;
import hu.bme.mit.gamma.querygenerator.serializer.XSTSUppaalPropertySerializer;
import hu.bme.mit.gamma.theta.verification.ThetaVerifier;
import hu.bme.mit.gamma.trace.model.ExecutionTrace;
import hu.bme.mit.gamma.trace.testgeneration.java.TestGenerator;
import hu.bme.mit.gamma.trace.util.TraceUtil;
import hu.bme.mit.gamma.api.taskhandler.AbstractVerification;
import hu.bme.mit.gamma.api.taskhandler.TaskHandler;
import hu.bme.mit.gamma.api.taskhandler.ThetaVerification;
import hu.bme.mit.gamma.api.taskhandler.UppaalVerification;
import hu.bme.mit.gamma.api.taskhandler.XSTSUppaalVerification;
import hu.bme.mit.gamma.uppaal.verification.UppaalVerifier;
import hu.bme.mit.gamma.util.FileUtil;
import hu.bme.mit.gamma.util.GammaEcoreUtil;

public class VerificationHandler extends TaskHandler {

	protected String testFolderUri;
	protected TraceUtil traceUtil = TraceUtil.INSTANCE;
	
	protected Logger logger = Logger.getLogger("GammaLogger");
	
	public VerificationHandler(IFile file) {
		super(file);
	}
	
	public void execute(Verification verification) throws IOException {
		setVerification(verification);
		Set<AnalysisLanguage> languagesSet = new HashSet<AnalysisLanguage>(verification.getLanguages());
		checkArgument(languagesSet.size() == 1);
		AbstractVerification verificationTask = null;
		PropertySerializer propertySerializer = null;
		for (AnalysisLanguage analysisLanguage : languagesSet) {
			switch (analysisLanguage) {
				case UPPAAL:
					verificationTask = UppaalVerification.INSTANCE;
					propertySerializer = UppaalPropertySerializer.INSTANCE;
					break;
				case THETA:
					verificationTask = ThetaVerification.INSTANCE;
					propertySerializer = ThetaPropertySerializer.INSTANCE;
					break;
				case XSTS_UPPAAL:
					verificationTask = XSTSUppaalVerification.INSTANCE;
					propertySerializer = XSTSUppaalPropertySerializer.INSTANCE;
					break;
				default:
					throw new IllegalArgumentException("Currently only UPPAAL and Theta are supported.");
			}
		}
		String filePath = verification.getFileName().get(0);
		File modelFile = new File(filePath);
		
		List<String> queryFileLocations = new ArrayList<String>();
		// String locations
		queryFileLocations.addAll(verification.getQueryFiles());
		// Serializing property models
		for (PropertyPackage propertyPackage : verification.getPropertyPackages()) {
			File file = ecoreUtil.getFile(propertyPackage.eResource());
			String fileName = fileUtil.toHiddenFileName(fileUtil.changeExtension(file.getName(), "pd"));
			File newFile = new File(file.getParentFile().toString() + File.separator + fileName);
			StringBuilder formulas = new StringBuilder();
			for (CommentableStateFormula formula : propertyPackage.getFormulas()) {
				String serializedFormula = propertySerializer.serialize(formula);
				formulas.append(serializedFormula + System.lineSeparator());
			}
			fileUtil.saveString(newFile, formulas.toString());
			newFile.deleteOnExit();
			queryFileLocations.add(newFile.toString());
		}
		
		for (String queryFileLocation : queryFileLocations) {
			logger.log(Level.INFO, "Checking " + queryFileLocation + "...");
			File queryFile = new File(queryFileLocation);
			
			ExecutionTrace trace = verificationTask.execute(modelFile, queryFile);
			// Maybe there is no trace
			if (trace != null) {
				if (verification.isOptimize()) {
					logger.log(Level.INFO, "Optimizing trace...");
					traceUtil.removeCoveredSteps(trace);
				}
				
				String basePackage = verification.getPackageName().get(0);
				String traceFolder = targetFolderUri;
				
				Entry<String, Integer> fileNamePair = fileUtil.getFileName(new File(traceFolder), "ExecutionTrace", "get");
				String fileName = fileNamePair.getKey();
				Integer id = fileNamePair.getValue();
				saveModel(trace, traceFolder, fileName);
				
				String className = fileUtil.getExtensionlessName(fileName).replace(id.toString(), "");
				className += "Simulation" + id;
				TestGenerator testGenerator = new TestGenerator(trace, basePackage, className);
				String testCode = testGenerator.execute();
				String testFolder = testFolderUri;
				fileUtil.saveString(testFolder + File.separator + testGenerator.getPackageName().replaceAll("\\.", "/") +
					File.separator + className + ".java", testCode);
			}
		}
	}

	private void setVerification(Verification verification) {
		if (verification.getPackageName().isEmpty()) {
			verification.getPackageName().add(file.getProject().getName().toLowerCase());
		}
		if (verification.getTestFolder().isEmpty()) {
			verification.getTestFolder().add("test-gen");
		}
		// Setting the attribute, the test folder is a RELATIVE path now from the project
		testFolderUri = URI.decode(projectLocation + File.separator + verification.getTestFolder().get(0));
		File file = ecoreUtil.getFile(verification.eResource()).getParentFile();
		logger.info("LOGGGGGG" + file.getPath());
		// Setting the file paths
		verification.getFileName().replaceAll(it -> fileUtil.exploreRelativeFile(file, it).toString());
		// Setting the query paths
		verification.getQueryFiles().replaceAll(it -> fileUtil.exploreRelativeFile(file, it).toString());
	}
	
}

abstract class AbstractVerification {

	protected FileUtil fileUtil = FileUtil.INSTANCE;
	protected GammaEcoreUtil ecoreUtil = GammaEcoreUtil.INSTANCE;
	public abstract ExecutionTrace execute(File modelFile, File queryFile);
	
}

class UppaalVerification extends AbstractVerification {
	// Singleton
	public static final UppaalVerification INSTANCE = new UppaalVerification();
	protected UppaalVerification() {}
	//
	@Override
	public ExecutionTrace execute(File modelFile, File queryFile) {
		String packageFileName =
				fileUtil.toHiddenFileName(fileUtil.changeExtension(modelFile.getName(), "g2u"));
		EObject gammaTrace = ecoreUtil.normalLoad(modelFile.getParent(), packageFileName);
		UppaalVerifier verifier = new UppaalVerifier();
		return verifier.verifyQuery(gammaTrace, "-C -T -t0", modelFile, queryFile, true, true);
	}

}

class XSTSUppaalVerification extends AbstractVerification {
	// Singleton
	public static final XSTSUppaalVerification INSTANCE = new XSTSUppaalVerification();
	protected XSTSUppaalVerification() {}
	//
	@Override
	public ExecutionTrace execute(File modelFile, File queryFile) {
		String packageFileName =
				fileUtil.toHiddenFileName(fileUtil.changeExtension(modelFile.getName(), "gsm"));
		EObject gammaPackage = ecoreUtil.normalLoad(modelFile.getParent(), packageFileName);
		UppaalVerifier verifier = new UppaalVerifier();
		return verifier.verifyQuery(gammaPackage, "-C -T -t0", modelFile, queryFile, true, true);
	}

}

class ThetaVerification extends AbstractVerification {
	// Singleton
	public static final ThetaVerification INSTANCE = new ThetaVerification();
	protected ThetaVerification() {}
	//
	@Override
	public ExecutionTrace execute(File modelFile, File queryFile) {
		String packageFileName =
				fileUtil.toHiddenFileName(fileUtil.changeExtension(modelFile.getName(), "gsm"));
		EObject gammaPackage = ecoreUtil.normalLoad(modelFile.getParent(), packageFileName);
		ThetaVerifier verifier = new ThetaVerifier();
		String queries = fileUtil.loadString(queryFile);
		return verifier.verifyQuery(gammaPackage, "", modelFile, queries, true, true);
	}
	
}
