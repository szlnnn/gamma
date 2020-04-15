/********************************************************************************
 * Copyright (c) 2018 Contributors to the Gamma project
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package hu.bme.mit.gamma.uppaal.backannotation

import hu.bme.mit.gamma.expression.model.EnumerationLiteralExpression
import hu.bme.mit.gamma.expression.model.IntegerLiteralExpression

class ExpressionSerializer extends hu.bme.mit.gamma.codegenerator.java.util.ExpressionSerializer {
	
	override dispatch String serialize(EnumerationLiteralExpression expression) {
		return  "\"" + expression.reference.name + "\"";
	}
	
	override dispatch String serialize(IntegerLiteralExpression expression) {
		return "(long) " + expression.value.toString
	}
	
	
}
