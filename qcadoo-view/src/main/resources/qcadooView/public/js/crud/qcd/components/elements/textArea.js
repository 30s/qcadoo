/*
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.2.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
var QCD = QCD || {};
QCD.components = QCD.components || {};
QCD.components.elements = QCD.components.elements || {};

QCD.components.elements.TextArea = function(_element, _mainController) {
	$.extend(this, new QCD.components.elements.FormComponent(_element, _mainController));

	var _this = this;
	
	var hasListeners = (this.options.listeners && this.options.listeners.length > 0)

	if (this.options.referenceName) {
		_mainController.registerReferenceName(this.options.referenceName, this);
	}

	if (this.input) {
		this.input.change(inputDataChanged);
	}

	function inputDataChanged() {
		var inputData = _this.getComponentData();
		_this.fireOnChangeListeners("onChange", [inputData.value]);
		if (hasListeners) {
			_mainController.callEvent("onChange", elementPath, null, null, null);
		}
	}

	this.updateSize = function(_width, _height) {
		var height = _height ? _height - 10 : 90;
		if (height < 50) {
		    // same as input['text']
			this.input.height(22);
		} else {
			this.input.height(height - 23);
		}
		this.input.parent().parent().parent().height(height);
	}
}