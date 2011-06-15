/*
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.2
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
QCD.menu = QCD.menu || {};

QCD.menu.MenuModel = function(menuStructure) {
	
	function addCategory(model, item, type) {
		var button = new QCD.menu.FirstButton(item, type);
		model.items.push(button);
		model.itemsMap[button.name] = button;
		if (! model.selectedItem) {
			model.selectedItem = button;
			button.selectedItem = button.items[0]; 
		}
	}
	
	this.selectedItem = null;
	this.items = new Array();
	this.itemsMap = new Object();
	
	addCategory(this, menuStructure.homeCategory, QCD.menu.MenuModel.HOME_CATEGORY);
	for (var i in menuStructure.menuItems) {
		addCategory(this, menuStructure.menuItems[i]);
	}
	addCategory(this, menuStructure.administrationCategory, QCD.menu.MenuModel.ADMINISTRATION_CATEGORY);
}

QCD.menu.MenuModel.HOME_CATEGORY = 1;
QCD.menu.MenuModel.ADMINISTRATION_CATEGORY = 2;
QCD.menu.MenuModel.REGULAR_CATEGORY = 3;

QCD.menu.FirstButton = function(menuItem, menuItemType) {
	this.type = menuItemType ? menuItemType : QCD.menu.MenuModel.REGULAR_CATEGORY;
	this.name = menuItem.name;
	this.label = menuItem.label;
	
	this.element = null;
	
	this.selectedItem = null;
	
	this.itemsMap = new Object();
	this.items = new Array();
	for (var i in menuItem.items) {
		var secondButton = new QCD.menu.SecondButton(menuItem.items[i], this);
		this.itemsMap[secondButton.name] = secondButton;
		this.items.push(secondButton);
	}
}

QCD.menu.SecondButton = function(menuItem, firstButton) {
	this.name = firstButton.name+"_"+menuItem.name;
	this.label = menuItem.label;
	
	this.page = menuItem.page;
	
	this.element = null;
	
}