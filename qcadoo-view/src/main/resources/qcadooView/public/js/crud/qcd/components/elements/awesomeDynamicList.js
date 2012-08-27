/*
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 1.1.7
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

QCD.components.elements.AwesomeDynamicList = function(_element, _mainController) {
	$.extend(this, new QCD.components.Container(_element, _mainController));
	
	var mainController = _mainController;
	var elementPath = this.elementPath;
	var elementSearchName = this.elementSearchName;
	
	var innerFormContainer;
	var awesomeDynamicListContent;
	
	var awesomeDynamicListHeader;
	var awesomeDynamicListHeaderObject;
	
	var formObjects;
	var formObjectsIndex = 1;
	
	var currentWidth;
	var currentHeight;
	
	var buttonsArray = new Array();
	
	var firstLine;
	
	var BUTTONS_WIDTH = 70;
	
	var hasButtons = this.options.hasButtons;
	
	var enabled = true;
	
	var isChanged = false;
	
	var components = new Object();
	
	var isRequired = false;
	
	function constructor(_this) {
		innerFormContainer = $("#"+_this.elementSearchName+" > .awesomeDynamicList > .awesomeDynamicListInnerForm").children();
				
		awesomeDynamicListContent = $("#"+_this.elementSearchName+" > .awesomeDynamicList > .awesomeDynamicListContent");
		awesomeDynamicListHeader = $("#"+_this.elementSearchName+" > .awesomeDynamicList > .awesomeDynamicListHeader");
		if (awesomeDynamicListHeader && awesomeDynamicListHeader.length > 0) {
			awesomeDynamicListHeaderObject = QCDPageConstructor.getChildrenComponents(awesomeDynamicListHeader.children(), mainController)["header"];
			awesomeDynamicListHeaderObject.setEnabled(true, true);
		}
		
		formObjects = new Array();
		formObjectsMap = new Object();
		if (!hasButtons) {
			BUTTONS_WIDTH = 0;
		}
		
		_this.components = components;
		
		updateButtons();
	}
	
	this.getComponentValue = function() {
		var formValues = new Array();
		for (var i in formObjects) {
			if (! formObjects[i]) {
				continue;
			}
			formValues.push({
				name: formObjects[i].elementName,
				value: formObjects[i].getValue()
			});
		}
		return { 
			forms: formValues
		};
	}
	
	this.setComponentValue = function(value) {
		var forms = value.forms;
		if (value.required != undefined) {
			isRequired = value.required;
		}
		if (forms) {
			formObjects = new Array();
			awesomeDynamicListContent.empty();	
			this.components = new Object();
			components = this.components;
			formObjectsIndex = 1;
			for (var i in forms) {
				var formValue = forms[i];
				var formObject = getFormCopy(formObjectsIndex);
				formObject.setValue(formValue);
				formObjects[formObjectsIndex] = formObject;
				this.components[formObject.elementName] = formObject;
				formObjectsIndex++;
			}
			
			if (isRequired && formObjectsIndex == 1) {
				var formObject = getFormCopy(formObjectsIndex, true);
				formObjects[formObjectsIndex] = formObject;
				this.components[formObject.elementName] = formObject;
				formObjectsIndex++;
			}
			
			updateButtons();
		} else {
			var innerFormChanges = value.innerFormChanges;
			for (var i in innerFormChanges) {
				this.components[i].setValue(innerFormChanges[i]);
			}
		}
		mainController.updateSize();
	}
	
	this.setComponentState = function(state) {
		this.setComponentValue(state);
	}
	
	this.setComponentEnabled = function(isEnabled) {
		enabled = isEnabled;
		if (isEnabled) {
			for (var i in buttonsArray) {
				if (buttonsArray[i]) {
					buttonsArray[i].addClass("enabled");
				}
			}
		} else {
			for (var i in buttonsArray) {
				if (buttonsArray[i]) {
					buttonsArray[i].removeClass("enabled");
				}
			}
		}
	}
	
	this.isComponentChanged = function() {
		if (isChanged) {
			return true;
		}
		for (var i in formObjects) {
			if (formObjects[i] && !formObjects[i].isVirtual && formObjects[i].isChanged()) {
				return true;
			}
		}
		return false;
	}
	
	this.performUpdateState = function() {
		isChanged = false;
	}
	
	this.setComponentLoading = function(isLoadingVisible) {
	}
	
	this.updateSize = function(_width, _height) {
		currentWidth = _width;
		currentHeight = _height;
		for (var i in formObjects) {
			if (formObjects[i]) {
				formObjects[i].updateSize(_width-BUTTONS_WIDTH, _height);
			}
		}
		if (awesomeDynamicListHeaderObject) {
			awesomeDynamicListHeader.width(_width-BUTTONS_WIDTH-20);
			awesomeDynamicListHeaderObject.updateSize(_width-BUTTONS_WIDTH-30, _height);
		}
		
		$(".awesomeListLine").addClass('forceRedraw').removeClass('forceRedraw'); // IE fix - force redraw
	}
	
	function getFormCopy(formId, isVirtual) {
		isVirtual = isVirtual ? isVirtual : false;
		var copy = innerFormContainer.clone();
		
		changeElementId(copy, formId);
		var line = $("<div>").addClass("awesomeListLine").attr("id", elementPath+"_line_"+formId);
		var formContainer = $("<span>").addClass("awesomeListFormContainer");
		formContainer.append(copy);
		line.append(formContainer);
		if (hasButtons) {
			var buttons = $("<span>").addClass("awesomeListButtons");
		
			var removeLineButton = $("<a>").addClass("awesomeListButton").addClass("awesomeListMinusButton").addClass("enabled").attr("id", elementPath+"_line_"+formId+"_removeButton");
			removeLineButton.css("display", "none");
			removeLineButton.click(function(e) {
				var button = $(e.target);
				if (button.hasClass("enabled")) {
					var lineId = button.attr("id").substring(elementPath.length+6, button.attr("id").length-13); 
					removeRowClicked(lineId);
				}
			});
			buttonsArray.push(removeLineButton);
			buttons.append(removeLineButton);
			var addLineButton = $("<a>").addClass("awesomeListButton").addClass("awesomeListPlusButton").addClass("enabled").attr("id", elementPath+"_line_"+formId+"_addButton");
			addLineButton.click(function(e) {
				var button = $(e.target);
				if (button.hasClass("enabled")) {
					var lineId = button.attr("id").substring(elementPath.length+6, button.attr("id").length-10); 
					addRowClicked(lineId);
				}
			});
			addLineButton.css("display", "none");
			buttonsArray.push(addLineButton);
			buttons.append(addLineButton);
			
			line.append(buttons);
		}
		awesomeDynamicListContent.append(line);
		var formObject = QCDPageConstructor.getChildrenComponents(copy, mainController)["innerForm_"+formId];
		
		formObject.isVirtual = isVirtual;
		
		formObject.updateSize(currentWidth-BUTTONS_WIDTH, currentHeight);
		formObject.setValue(deleteContentFromField(formObject.getValue()));
		
		return formObject;
	}
	
	function deleteContentFromField(value){
		if(value.content){
			value.content = null;
		}
		for (var i in value.components) {
			var formObj = formObjects[i];
			if (! formObj) {
				continue;
			}else{
			deleteContentFromField(formObj);
			}
		}
		return value;
	}
	
	function addRowClicked(rowId) {
		var formObject = getFormCopy(formObjectsIndex);
		formObjects[formObjectsIndex] = formObject;
		components[formObject.elementName] = formObject;
		isChanged = true;
		formObjectsIndex++;
		updateButtons();
		mainController.updateSize();
	}
	
	function removeRowClicked(rowId) {
		var line = $("#"+elementSearchName+"_line_"+rowId);
		line.remove();
		isChanged = true;
		formObjects[rowId] = null;
		updateButtons();
		mainController.updateSize();
	}
	
	function updateButtons() {
		if (!hasButtons) {
			return;
		}
		var objectCounter = 0;
		var lastObject = 0;
		for (var i in formObjects) {
			if (formObjects[i]) {
				objectCounter++;
				lastObject = i;
			}
		}
		if (objectCounter  > 0) {
			if (firstLine) {
				firstLine.hide();
				firstLine = null;
			}
			for (var i in formObjects) {
				if (! formObjects[i]) {
					continue;
				}
				var line = $("#"+elementSearchName+"_line_"+i);
				var removeButton = $("#"+elementSearchName+"_line_"+i+"_removeButton");
				var addButton = $("#"+elementSearchName+"_line_"+i+"_addButton");
				if (!(isRequired && objectCounter<=1)){
					removeButton.show();
					removeButton.css("display", "inline-block");
				} else {
					removeButton.hide();
				}
				if (i == lastObject) {
					addButton.show();
					addButton.css("display", "inline-block");
					line.addClass("lastLine");
				} else {
					addButton.hide();
					line.removeClass("lastLine");
				}
			}
		} else {
			firstLine = $("<div>").addClass("awesomeListLine").addClass("lastLine").attr("id", elementPath+"_line_0");
			var buttons = $("<span>").addClass("awesomeListButtons");
			var addLineButton = $("<a>").addClass("awesomeListButton").addClass("awesomeListPlusButton").attr("id", elementPath+"_line_0_addButton");
			addLineButton.click(function(e) {
				var button = $(e.target);
				if (button.hasClass("enabled")) {
					var lineId = button.attr("id").substring(elementPath.length+6, button.attr("id").length-10); 
					addRowClicked(lineId);
				}
			});
			buttons.append(addLineButton);
			buttonsArray.push(addLineButton);
			if (enabled) {
				addLineButton.addClass("enabled");
			}
			firstLine.append(buttons);
			awesomeDynamicListContent.append(firstLine);
		}
	}
	
	
	
	function changeElementId(element, formId) {
		var id = element.attr("id");
		if (id) {
			element.attr("id",id.replace("@innerFormId", formId));
		}
		element.children().each(function(i,e) {
			var kid = $(e);
			changeElementId(kid, formId)
		});
	}
	
	constructor(this);
}