/********************************************************************************
 * Copyright (c) 2019 Contributors to the Gamma project
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

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;

import hu.bme.mit.gamma.genmodel.model.YakinduCompilation;
import hu.bme.mit.gamma.api.taskhandler.TaskHandler;

public abstract class YakinduCompilationHandler extends TaskHandler {
	
	public YakinduCompilationHandler(IFile file) {
		super(file);
	}
	
	protected void setYakinduCompilation(YakinduCompilation yakinduCompilation) {
		String fileName = getNameWithoutExtension(getContainingFileName(yakinduCompilation.getStatechart()));
		checkArgument(yakinduCompilation.getFileName().size() <= 1);
		checkArgument(yakinduCompilation.getPackageName().size() <= 1);
		if (yakinduCompilation.getFileName().isEmpty()) {
			yakinduCompilation.getFileName().add(fileName);
		}
		if (yakinduCompilation.getPackageName().isEmpty()) {
			yakinduCompilation.getPackageName().add(yakinduCompilation.getStatechart().getName().toLowerCase());
		}
	}
	
	
	protected String getContainingFileName(EObject object) {
		System.out.println(object.eResource());
		return object.eResource().getURI().lastSegment();
	}
	
	
}
