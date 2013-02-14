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

QCD.components.elements.Grid = function(_element, _mainController) {
	$.extend(this, new QCD.components.Component(_element, _mainController));

	var mainController = _mainController;
	var element = _element;

	var options = this.options;

	var headerController;

	var elementPath = this.elementPath;
	var elementName = this.elementName;
	var elementSearchName = this.elementSearchName;

	var gridParameters;
	var grid;
	var belongsToFieldName;
	var currentOrder;

	var translations;

	var componentEnabled = false;

	var currentGridHeight;

	var linkListener;

	var selectAllCheckBox;

	var currentState = {
		selectedEntityId : null,
		selectedEntities : new Object(),
		filtersEnabled : true,
		newButtonClickedBefore : false,
		addExistingButtonClickedBefore : false,
		multiselectMode : true,
		isEditable : true
	}

	var RESIZE_COLUMNS_ON_UPDATE_SIZE = true;

	var columnModel = new Object();

	var hiddenColumnValues = new Object();

	var defaultOptions = {
		paging : true,
		fullScreen : false,
		shrinkToFit : false
	};

	var globalColumnTranslations = {};

	var currentEntities;

	var noRecordsDiv;

	var FILTER_TIMEOUT = 200;
	var filterRefreshTimeout = null;

	var fireOnChangeListeners = this.fireOnChangeListeners;

	var _this = this;

	var lookupWindow;

	var addedEntityId;

	if (this.options.referenceName) {
		mainController.registerReferenceName(this.options.referenceName, this);
	}

	function parseOptions(options) {
		gridParameters = new Object();

		var colNames = new Array();
		var colModel = new Array();
		var hasFilterableColumns = false;

		for ( var i in options.columns) {
			var column = options.columns[i];
			columnModel[column.name] = column;
			var isSortable = false;
			var isSerchable = false;
			for ( var sortColIter in options.orderableColumns) {
				if (options.orderableColumns[sortColIter] == column.name) {
					isSortable = true;
					break;
				}
			}
			for ( var sortColIter in options.searchableColumns) {
				if (options.searchableColumns[sortColIter] == column.name) {
					isSerchable = true;
					hasFilterableColumns = true;
					break;
				}
			}

			column.isSerchable = isSerchable;

			if (!column.hidden) {
				if (isSortable) {
					colNames.push(column.label + "<div class='sortArrow' id='"
							+ elementPath + "_sortArrow_" + column.name
							+ "'></div>");
				} else {
					colNames.push(column.label);
				}

				var stype = 'text';
				var searchoptions = {};
				var possibleValues = new Object();
				if (column.filterValues) {
					possibleValues[""] = "";
					var possibleValuesString = ":"
					for ( var i in column.filterValues) {
						possibleValues[i] = column.filterValues[i];
						possibleValuesString += ";" + i + ":"
								+ column.filterValues[i];
					}
					stype = 'select';
					searchoptions.value = possibleValuesString;
					searchoptions.defaultValue = "";
				}

				var col = {
					name : column.name,
					index : column.name,
					width : column.width,
					sortable : isSortable,
					resizable : true,
					align : column.align,
					stype : stype,
					searchoptions : searchoptions
				};

				if (searchoptions.value) {
					globalColumnTranslations[column.name] = possibleValues;
					col.formatter = function(cellvalue, options, rowObject) {
						return globalColumnTranslations[options.colModel.name][cellvalue];
					}
				}

				colModel.push(col);
			} else {
				hiddenColumnValues[column.name] = new Object();
			}
		}

		gridParameters.hasFilterableColumns = hasFilterableColumns;
		gridParameters.filtersDefaultEnabled = options.filtersDefaultVisible && hasFilterableColumns;
		gridParameters.hasPredefinedFilters = options.hasPredefinedFilters;
		gridParameters.predefinedFilters = options.predefinedFilters;

		gridParameters.sortColumns = options.orderableColumns;

		gridParameters.colNames = colNames;
		gridParameters.colModel = colModel;
		gridParameters.datatype = function(postdata) {}
		gridParameters.multiselect = true;
		gridParameters.shrinkToFit = true;

		gridParameters.listeners = options.listeners;
		gridParameters.canNew = options.creatable;
		gridParameters.canDelete = options.deletable;
		gridParameters.paging = options.paginable;
		gridParameters.activable = options.activable;
		gridParameters.lookup = options.lookup;
		gridParameters.filter = hasFilterableColumns;
		gridParameters.orderable = options.prioritizable;
		gridParameters.allowMultiselect = options.multiselect;

		gridParameters.fullScreen = options.fullscreen;
		if (options.height) {
			gridParameters.height = parseInt(options.height);
			if (gridParameters.height <= 0) {
				gridParameters.height = null;
			}
		}
		if (options.width) {
			gridParameters.width = parseInt(options.width);
		}
		if (!gridParameters.width && !gridParameters.fullScreen) {
			gridParameters.width = 300;
		}
		gridParameters.correspondingViewName = options.correspondingView;
		gridParameters.correspondingComponent = options.correspondingComponent;
		gridParameters.correspondingLookup = options.correspondingLookup;
		gridParameters.correspondingViewInModal = options.correspondingViewInModal;
		gridParameters.weakRelation = options.weakRelation

		for ( var opName in defaultOptions) {
			if (gridParameters[opName] == undefined) {
				gridParameters[opName] = defaultOptions[opName];
			}
		}

	}

	function rowClicked(rowId, col) {

		if (!componentEnabled || !currentState.isEditable) {
			grid.setSelection(rowId, false);
			return;
		}

		if (currentState.selectedEntities[rowId]) {
			if (col == 0 && currentState.multiselectMode) {
				currentState.selectedEntities[rowId] = null;
			} else if (currentState.multiselectMode) {
				// diselect all but this
				for ( var i in currentState.selectedEntities) {
					if (currentState.selectedEntities[i]) {
						grid.setSelection(i, false);
						currentState.selectedEntities[i] = null;
					}
				}
				currentState.selectedEntities[rowId] = true;
			} else {
				currentState.selectedEntities[rowId] = null;
			}
		} else {
			if (col == 0 && gridParameters.allowMultiselect) {
				// do nothing
			} else {
				// diselect all
				for ( var i in currentState.selectedEntities) {
					if (currentState.selectedEntities[i]) {
						grid.setSelection(i, false);
						currentState.selectedEntities[i] = null;
					}
				}
			}
			currentState.selectedEntities[rowId] = true;
		}

		aferSelectionUpdate();

		// FIRE JAVA LISTENERS
		if (gridParameters.listeners.length > 0) {
			onSelectChange();
		}
	}

	function aferSelectionUpdate() {
		var selectionCounter = 0;
		var lastSelectedRow = null;
		var selectedArray = new Array();
		var selectedEntitiesArray = new Array();
		for ( var i in currentState.selectedEntities) {
			if (i == "undefined") {
				currentState.selectedEntities = false;
				continue;
			}
			if (currentState.selectedEntities[i]) {
				selectionCounter++;
				lastSelectedRow = i;
				selectedArray.push(i);
				selectedEntitiesArray.push(currentEntities[i]);
			}
		}

		if (selectionCounter == 0) {
			currentState.selectedEntities = new Object();
			currentState.multiselectMode = false;
			currentState.selectedEntityId = null;
		} else if (selectionCounter == 1) {
			currentState.multiselectMode = false;
			currentState.selectedEntityId = lastSelectedRow;
		} else {
			currentState.multiselectMode = true;
			currentState.selectedEntityId = null;
		}

		// UPDATE SELECTION COLOR
		if (currentState.multiselectMode) {
			element.addClass("multiselectMode");
		} else {
			element.removeClass("multiselectMode");
		}

		// UPDATE SELECT ALL BUTTON
		if (selectAllCheckBox) {
			var isAllSelected = true;
			var isEmpty = true;
			for ( var i in currentEntities) {
				isEmpty = false;
				if (currentState.selectedEntities[i] != true) {
					isAllSelected = false;
					break;
				}
			}
			if (isEmpty) {
				isAllSelected = false;
			}
			if (isAllSelected) {
				selectAllCheckBox.attr('checked', true);
				selectAllCheckBox.attr('title', translations.diselectAll);
			} else {
				selectAllCheckBox.attr('checked', false);
				selectAllCheckBox.attr('title', translations.selectAll);
			}
		}

		// UPDATE HEADER
		if (currentState.multiselectMode) {
			headerController.onSelectionChange(true);
		} else {
			var rowIndex = null;
			if (currentState.selectedEntityId) {
				rowIndex = grid.jqGrid('getInd', currentState.selectedEntityId);
				if (rowIndex == false) {
					rowIndex = null;
				}
			}
			headerController.onSelectionChange(false, rowIndex);
		}

		// FIRE ON CHANGE LISTENERS
		fireOnChangeListeners("onChange", [ selectedEntitiesArray ]);
	}

	this.setLinkListener = function(_linkListener) {
		linkListener = _linkListener;
	}

	function linkClicked(selectedEntities) {
		if (!currentState.isEditable) {
			return;
		}
		if (linkListener) {
			linkListener.onGridLinkClicked(selectedEntities);
		} else {
			var params = {};
			params[gridParameters.correspondingComponent + ".id"] = selectedEntities;
			redirectToCorrespondingPage(params);
		}
	}

	function redirectToCorrespondingPage(params) {
		if (gridParameters.correspondingViewName
				&& gridParameters.correspondingViewName != '' && mainController.canClose()) {
			setPermanentlyDisableParam(params);
			params[gridParameters.correspondingComponent + "."
					+ belongsToFieldName] = currentState.belongsToEntityId;
			var url = gridParameters.correspondingViewName + ".html?context="
					+ JSON.stringify(params);
			if (gridParameters.correspondingViewInModal) {
				mainController.openModal(elementPath + "_editWindow", url);
			} else {
				mainController.goToPage(url);
			}
		}
	}
	
	function showCorrespondingLookupGridModal(params) {
		if (gridParameters.correspondingLookup
				&& gridParameters.correspondingLookup != '' && mainController.canClose()) {
			setPermanentlyDisableParam(params);
			var correspondingLookupComponent = mainController.getComponentByReferenceName(gridParameters.correspondingLookup);
			var url = pluginIdentifier + "/" + correspondingLookupComponent.options.viewName + ".html?context="
					+ JSON.stringify(params);
			lookupWindow = mainController.openModal(elementPath + "_editWindow",
					url, false, onModalClose, onModalRender);	
		}
	}
	
	function setPermanentlyDisableParam(params) {
		if (!componentEnabled) {
			params["window.permanentlyDisabled"] = true;
		}
	}

	this.getComponentValue = function() {
		return currentState;
	}

	this.setComponentState = function(state) {
		currentState.selectedEntityId = state.selectedEntityId;
		currentState.selectedEntities = state.selectedEntities;
		currentState.multiselectMode = state.multiselectMode;
		currentState.onlyActive = state.onlyActive;

		if (state.belongsToEntityId) {
			currentState.belongsToEntityId = state.belongsToEntityId;
		} else {
			currentState.belongsToEntityId = null;
		}
		if (state.firstEntity) {
			currentState.firstEntity = state.firstEntity;
		}
		if (state.maxEntities) {
			currentState.maxEntities = state.maxEntities;
		}
		if (currentState.filtersEnabled != state.filtersEnabled) {
			currentState.filtersEnabled = state.filtersEnabled;
			grid[0].toggleToolbar();
			updateSearchFields();
			if (currentState.filtersEnabled) {
				headerController.setFilterActive();
				currentGridHeight -= 21;
			} else {
				headerController.setFilterNotActive();
				currentGridHeight += 21;
			}
			grid.setGridHeight(currentGridHeight);
		}
		if (state.order) {
			setSortColumnAndDirection(state.order);
		}
		if (state.filters) {
			currentState.filters = state.filters;
			for ( var filterIndex in currentState.filters) {
				$("#gs_" + filterIndex).val(currentState.filters[filterIndex]);
			}
			findMatchingPredefiniedFilter();
			onFiltersStateChange();
		}
		if (state.newButtonClickedBefore || state.addExistingButtonClickedBefore) {
			var lastPageController = mainController.getLastPageController();
			if (lastPageController
					&& lastPageController.getViewName() == gridParameters.correspondingViewName) {
				var lastCorrespondingComponent = lastPageController
						.getComponentByReferenceName(gridParameters.correspondingComponent);
				addedEntityId = lastCorrespondingComponent.getComponentValue().entityId;
			}
		}
	}

	this.setComponentValue = function(value) {
		currentState.selectedEntityId = value.selectedEntityId;

		if (value.belongsToEntityId) {
			currentState.belongsToEntityId = value.belongsToEntityId;
		} else {
			currentState.belongsToEntityId = null;
		}
		if (value.firstEntity) {
			currentState.firstEntity = value.firstEntity;
		}
		if (value.maxEntities) {
			currentState.maxEntities = value.maxEntities;
		}

		if (value.isEditable) {
			currentState.isEditable = value.isEditable;
		}

		if (value.entities == null) {
			return;
		}

		grid.jqGrid('clearGridData');
		var rowCounter = 1;
		currentEntities = new Object();
		for ( var entityNo in value.entities) {
			var entity = value.entities[entityNo];
			currentEntities[entity.id] = entity;
			var fields = new Object();
			for ( var fieldName in columnModel) {
				if (hiddenColumnValues[fieldName]) {
					hiddenColumnValues[fieldName][entity.id] = entity.fields[fieldName];
				} else {
					if (columnModel[fieldName].link && entity.fields[fieldName]
							&& entity.fields[fieldName] != "") {
						fields[fieldName] = "<a href=# id='" + elementPath
								+ "_" + fieldName + "_" + entity.id
								+ "' class='" + elementPath
								+ "_link gridLink'>" + entity.fields[fieldName]
								+ "</a>";

					} else {
						if (entity.fields[fieldName]
								&& entity.fields[fieldName] != "") {
							fields[fieldName] = entity.fields[fieldName];
						} else {
							fields[fieldName] = "";
						}
					}
				}
			}
			grid.jqGrid('addRowData', entity.id, fields);
			if (rowCounter % 2 == 0) {
				grid.jqGrid('setRowData', entity.id, false, "darkRow");
			} else {
				grid.jqGrid('setRowData', entity.id, false, "lightRow");
			}
			if (!entity.active) {
				grid.jqGrid('setRowData', entity.id, false, "inactive");
			}
			rowCounter++;
		}

		if (rowCounter == 1) {
			noRecordsDiv.show();
		} else {
			noRecordsDiv.hide();
		}

		$("." + elementSearchName + "_link").click(function(e) {
			var idArr = e.target.id.split("_");
			var entityId = idArr[idArr.length - 1];
			linkClicked(entityId);
		});

		headerController.updatePagingParameters(currentState.firstEntity,
				currentState.maxEntities, value.totalEntities);

		currentState.selectedEntities = value.selectedEntities;
		for ( var i in currentState.selectedEntities) {
			if (currentState.selectedEntities[i]) {
				var row = grid.getRowData(i)
				if (!row || jQuery.isEmptyObject(row)) {
					currentState.selectedEntities[i] = false;
				} else {
					grid.setSelection(i, false);
				}
			}
		}
		aferSelectionUpdate();

		if (value.order) {
			setSortColumnAndDirection(value.order);
		}

		if (value.entitiesToMarkAsNew) {
			for ( var i in value.entitiesToMarkAsNew) {
				var row = $("#" + elementSearchName + " #" + i);
				if (row) {
					row.addClass("lastAdded");
				}
			}
		}
		
		if (value.entitiesToMarkWithCssClass) {
			for (var styledEntityId in value.entitiesToMarkWithCssClass) {
				var row = $("#" + elementSearchName + " #" + styledEntityId);
				if (row) {
					var entityCssClasses = value.entitiesToMarkWithCssClass[styledEntityId];
					var entityCssClassesLen = entityCssClasses.length;
					for (var i = 0; i < entityCssClassesLen; i++) {
						row.addClass(entityCssClasses[i]);
					}
				}
			}
		}

		if (addedEntityId) {
			var row = $("#" + elementSearchName + " #" + addedEntityId);
			if (row) {
				row.addClass("lastAdded");
				addedEntityId = null;
			}
		}

		if (currentState.isEditable == false) {
			this.setComponentEditable(false);
		} else if (value.isEditable != undefined && value.isEditable != null) {
			this.setComponentEditable(value.isEditable);
		}

		unblockGrid();
	}

	this.setComponentEnabled = function(isEnabled) {
		componentEnabled = isEnabled;
		headerController.setEnabled(currentState.isEditable && isEnabled);
	}

	this.setComponentLoading = function(isLoadingVisible) {
		if (isLoadingVisible) {
			blockGrid();
		} else {
			unblockGrid();
		}
	}

	this.setComponentEditable = function(_isEditable) {
		currentState.isEditable = _isEditable;
		if (currentState.isEditable) {
			grid.removeClass("componentNotEditable");
		} else {
			grid.addClass("componentNotEditable");
		}
		headerController
				.setEnabled(currentState.isEditable && componentEnabled);
	}

	function blockGrid() {
		QCD.components.elements.utils.LoadingIndicator.blockElement(element);
	}

	function unblockGrid() {
		QCD.components.elements.utils.LoadingIndicator.unblockElement(element);
	}

	function constructor(_this) {

		parseOptions(_this.options, _this);

		gridParameters.modifiedPath = elementPath.replace(/\./g, "_");
		gridParameters.element = gridParameters.modifiedPath + "_grid";

		$("#" + elementSearchName + "_grid").attr('id', gridParameters.element);

		translations = _this.options.translations;
		belongsToFieldName = _this.options.belongsToFieldName;

		headerController = new QCD.components.elements.grid.GridHeaderController(
				_this, mainController, gridParameters,
				_this.options.translations);

		$("#" + elementSearchName + "_gridHeader").append(
				headerController.getHeaderElement());
		$("#" + elementSearchName + "_gridFooter").append(
				headerController.getFooterElement());

		currentState.firstEntity = headerController.getPagingParameters()[0];
		currentState.maxEntities = headerController.getPagingParameters()[1];

		gridParameters.onCellSelect = function(rowId, iCol, cellcontent, e) {
			rowClicked(rowId, iCol);
		}

		gridParameters.ondblClickRow = function(id) {}
		gridParameters.onSortCol = onSortColumnChange;

		grid = $("#" + gridParameters.element).jqGrid(gridParameters);

		$("#cb_" + gridParameters.element).hide(); // hide 'select add'
													// checkbox
		if (gridParameters.allowMultiselect) {
			selectAllCheckBox = $("<input type='checkbox'>");
			$("#" + elementSearchName + " #jqgh_cb").append(selectAllCheckBox);
			selectAllCheckBox.change(function() {
				onSelectAllClicked();
			});
		}

		for ( var i in gridParameters.sortColumns) {
			$(
					"#" + gridParameters.modifiedPath + "_grid_"
							+ gridParameters.sortColumns[i]).addClass(
					"sortableColumn");
		}

		element.width("100%");

		grid.jqGrid('filterToolbar', {
			stringResult : true
		});

		if (gridParameters.isLookup || gridParameters.filtersDefaultEnabled) {
			headerController.setFilterActive();
			currentState.filtersEnabled = true;
			$("#gs_" + options.columns[0].name).focus();
		} else {
			grid[0].toggleToolbar();
			currentState.filtersEnabled = false;
		}
		
		if (gridParameters.filtersDefaultEnabled) {
			updateSearchFields();
		}
		
		noRecordsDiv = $("<div>").html(translations.noResults).addClass(
				"noRecordsBox");
		noRecordsDiv.hide();
		$("#" + gridParameters.element).parent().append(noRecordsDiv);
		
	}

	this.onPagingParametersChange = function() {
		blockGrid();
		currentState.firstEntity = headerController.getPagingParameters()[0];
		currentState.maxEntities = headerController.getPagingParameters()[1];
		onCurrentStateChange();
	}

	function setSortColumnAndDirection(order) {
		if (currentOrder && currentOrder.column == order.column) {
			if (order.direction == "asc") {
				$("#" + elementSearchName + "_sortArrow_" + order.column)
						.removeClass("downArrow");
				$("#" + elementSearchName + "_sortArrow_" + order.column)
						.addClass("upArrow");
				currentState.order.direction = "asc";
			} else {
				$("#" + elementSearchName + "_sortArrow_" + order.column)
						.removeClass("upArrow");
				$("#" + elementSearchName + "_sortArrow_" + order.column)
						.addClass("downArrow");
				currentState.order.direction = "desc";
			}
		} else {
			if (currentOrder) {
				$(
						"#" + gridParameters.modifiedPath + "_grid_"
								+ currentOrder.column)
						.removeClass("sortColumn");
			}

			$("#" + gridParameters.modifiedPath + "_grid_" + order.column)
					.addClass("sortColumn");

			currentState.order = {
				column : order.column
			}

			if (order.direction == "asc") {
				$("#" + elementSearchName + "_sortArrow_" + order.column)
						.addClass("upArrow");
				currentState.order.direction = "asc";
			} else {
				$("#" + elementSearchName + "_sortArrow_" + order.column)
						.addClass("downArrow");
				currentState.order.direction = "desc";
			}
		}
		currentOrder = {
			column : order.column,
			direction : order.direction
		};
	}

	function onSelectAllClicked() {
		if (selectAllCheckBox.is(':checked')) {
			for ( var i in currentEntities) {
				if (currentState.selectedEntities[i] != true) {
					grid.setSelection(i, false);
					currentState.selectedEntities[i] = true;
				}
			}
		} else {
			for ( var i in currentState.selectedEntities) {
				if (currentState.selectedEntities[i]) {
					grid.setSelection(i, false);
					currentState.selectedEntities[i] = null;
				}
			}
		}
		aferSelectionUpdate();
		if (gridParameters.listeners.length > 0) {
			onSelectChange();
		}
	}

	function onSortColumnChange(index, iCol, sortorder) {
		blockGrid();
		currentState.order.column = index;
		if (currentState.order.direction == "asc") {
			currentState.order.direction = "desc";
		} else {
			currentState.order.direction = "asc";
		}
		onCurrentStateChange();
		return 'stop';
	}

	function onFilterChange() {
		performFilter();
	}

	function performFilter() {
		blockGrid();
		if (currentState.filtersEnabled) {
			currentState.filters = new Object();
			for ( var i in columnModel) {
				var column = columnModel[i];
				if (column.isSerchable) {
					var filterValue = $("#gs_" + column.name).val();
					filterValue = $.trim(filterValue);
					if (filterValue && filterValue != "") {
						currentState.filters[column.name] = filterValue;
					}
				}
			}
		} else {
			currentState.filters = null;
		}
		onCurrentStateChange();
		onFiltersStateChange();
	}

	this.onFilterButtonClicked = function() {
		grid[0].toggleToolbar();
		currentState.filtersEnabled = !currentState.filtersEnabled;
		if (currentState.filtersEnabled) {
			currentGridHeight -= 23;
			updateSearchFields();
			$("#gs_" + options.columns[0].name).focus();
		} else {
			currentGridHeight += 23;
		}
		grid.setGridHeight(currentGridHeight);
		onCurrentStateChange(true);
		onFiltersStateChange();
	}

	this.onClearFilterClicked = function() {
		currentState.filters = new Object();
		for ( var i in columnModel) {
			var column = columnModel[i];
			$("#gs_" + column.name).val("");
		}
		onFiltersStateChange();
		onCurrentStateChange();
	}

	function updateSearchFields() {
		for ( var i in columnModel) {
			var column = columnModel[i];
			if (column.isSerchable) {
				var columnElement = $("#gs_" + column.name);
				columnElement.unbind('change keyup');
				if (column.filterValues) {
					columnElement.change(onFilterChange);
				} else {
					columnElement.keyup(function(e) {
						var key = e.keyCode || e.which;
						if (key != 13) {
							return;
						}

						var val = $(this).val();
						var columnName = $(this).attr("id").substring(3);
						var currentFilter = "";
						if (currentState.filters
								&& currentState.filters[columnName]) {
							currentFilter = currentState.filters[columnName];
						}
						if (currentState.filters && val == currentFilter) {
							return;
						}
						onFilterChange();
					});
				}
			} else {
				$("#gs_" + column.name).hide();
			}
		}
	}

	this.setFilterState = function(column, filterText) {
		if (!currentState.filtersEnabled) {
			grid[0].toggleToolbar();
			currentState.filtersEnabled = true;
			headerController.setFilterActive();
			currentGridHeight -= 21;
			if (currentGridHeight) {
				grid.setGridHeight(currentGridHeight);
			}
		}
		currentState.filters = new Object();
		currentState.filters[column] = filterText;
		$("#gs_" + column).val(filterText);
		$("#gs_" + column).focus();
		updateSearchFields();
		onFiltersStateChange();
	}

	this.setOnlyActive = function(onlyActive) {
		blockGrid();
		currentState.onlyActive = onlyActive;
		onCurrentStateChange(gridParameters.hasPredefinedFilters);
	}

	this.setFilterObject = function(filter) {
		blockGrid();

		var filterObject = filter.filter
		for ( var i in columnModel) {
			var column = columnModel[i];
			$("#gs_" + column.name).val("");
		}
		var fieldsNo = 0;
		for ( var col in filterObject) {
			filterObject[col] = Encoder.htmlDecode(filterObject[col]);
			$("#gs_" + col).val(filterObject[col]);
			fieldsNo++;
		}
		currentState.filters = filterObject;

		if (fieldsNo == 0) {
			if (!gridParameters.filtersDefaultEnabled) {
				if (currentState.filtersEnabled) {
					currentGridHeight += 23;
					grid.setGridHeight(currentGridHeight);
					$(grid[0]).find('.ui-search-toolbar').hide();
				}
				headerController.setFilterNotActive();
				currentState.filtersEnabled = false;
			}
		} else {
			if (!currentState.filtersEnabled) {
				currentGridHeight -= 23;
				grid.setGridHeight(currentGridHeight);
				$(grid[0]).find('.ui-search-toolbar').show();
				$("#gs_" + options.columns[0].name).focus();
			
				headerController.setFilterActive();
				currentState.filtersEnabled = true;
			}
		}

		setSortColumnAndDirection( {
			column : filter.orderColumn,
			direction : filter.orderDirection
		});

		updateSearchFields();
		onFiltersStateChange();
		onCurrentStateChange(true);
	}

	this.onNewButtonClicked = function() {
		performNew();
	}

	this.onAddExistingButtonClicked = function() {
		showModalForAddExistingEntity();
	}

	this.onDeleteButtonClicked = function() {
		performDelete();
	}

	this.setDeleteEnabled = function(enabled) {
		headerController.setDeleteEnabled(enabled);
	}

	this.onUpButtonClicked = function() {
		blockGrid();
		mainController.callEvent("moveUp", elementPath, function() {
			unblockGrid();
		});
	}

	this.onDownButtonClicked = function() {
		blockGrid();
		mainController.callEvent("moveDown", elementPath, function() {
			unblockGrid();
		});
	}

	this.updateSize = function(_width, _height) {
		if (!_width) {
			_width = 300;
		}
		if (!_height) {
			_height = 300;
		}

		element.css("height", _height + "px")

		var HEIGHT_DIFF = 120;
		currentGridHeight = _height - HEIGHT_DIFF;
		if (currentState.filtersEnabled) {
			currentGridHeight -= 21;
		}
		if (!gridParameters.paging) {
			currentGridHeight += 35;
		}
		grid.setGridHeight(currentGridHeight);
		grid.setGridWidth(_width - 24, true);
	}

	function onFiltersStateChange() {
		var hasFiltersValues = false;
		for ( var i in currentState.filters) {
			if (currentState.filters[i] && currentState.filters[i] != "") {
				hasFiltersValues = true;
				break;
			}
		}
		if (hasFiltersValues) {
			headerController.setFiltersValuesNotEmpty();
		} else {
			headerController.setFiltersValuesEmpty();
		}
	}

	function onCurrentStateChange(forceUpdate) {
		currentState.selectedEntities = null;
		currentState.multiselectMode = false;
		currentState.selectedEntityId = null;
		if (!forceUpdate) {
			findMatchingPredefiniedFilter();
		}
		mainController.callEvent("refresh", elementPath, function() {
			unblockGrid();
		});
	}

	function findMatchingPredefiniedFilter() {
		var filterToSearch = {};
		if (currentState.filtersEnabled && currentState.filters) {
			filterToSearch = currentState.filters;
		}
		var isIdentical = true;
		for ( var i in gridParameters.predefinedFilters) {
			var predefiniedFilter = gridParameters.predefinedFilters[i].filter;
			isIdentical = true;

			if (gridParameters.predefinedFilters[i].orderColumn) {
				if (currentState.order.column != gridParameters.predefinedFilters[i].orderColumn) {
					isIdentical = false;
					continue;
				}
				if (currentState.order.direction != gridParameters.predefinedFilters[i].orderDirection) {
					isIdentical = false;
					continue;
				}
			}

			for ( var col in columnModel) {
				var column = columnModel[col];
				if (predefiniedFilter[column.name] != filterToSearch[column.name]) {
					isIdentical = false;
					break;
				}
			}
			if (isIdentical) {
				headerController.setPredefinedFilter(i);
				break;
			}
		}
		if (!isIdentical) {
			headerController.setPredefinedFilter(null);
		}
	}

	function onSelectChange() {
		if (componentEnabled) {
			mainController.callEvent("select", elementPath, null);
		}
	}

	this.performNew = function(actionsPerformer) {
		currentState.newButtonClickedBefore = true;
		currentState.selectedEntities = null;
		currentState.multiselectMode = false;
		currentState.selectedEntityId = null;

		redirectToCorrespondingPage({});
		if (actionsPerformer) {
			actionsPerformer.performNext();
		}
	}
	var performNew = this.performNew;

	this.performRefresh = function(actionsPerformer) {
		blockGrid();
		mainController.callEvent('refresh', elementPath, function() {
			unblockGrid();
		});
	}

	this.showModalForAddExistingEntity = function(actionsPerformer) {
		currentState.addExistingButtonClickedBefore = true;
		currentState.selectedEntities = null;
		currentState.multiselectMode = false;
		currentState.selectedEntityId = null;

		showCorrespondingLookupGridModal({});

		if (actionsPerformer) {
			actionsPerformer.performNext();
		}
	}
	var showModalForAddExistingEntity = this.showModalForAddExistingEntity;
	
	function onModalRender(modalWindow) {
		modalWindow.getComponent("window.grid").setLinkListener(_this);
	}

	this.onGridLinkClicked = function(selectedEntities) {
		performAddExistingEntity(null, selectedEntities);
		mainController.closeThisModalWindow();
	}

	function onModalClose() {
		lookupWindow = null;
	}

	function getSelectedRowsCount() {
		var selectionCounter = 0;
		for ( var i in currentState.selectedEntities) {
			if (currentState.selectedEntities[i]) {
				selectionCounter++;
			}
		}
		return selectionCounter;
	}

	this.performAddExistingEntity = function(actionsPerformer, selectedEntities) {
		blockGrid();
		mainController.callEvent("addExistingEntity", elementPath, function() {
			unblockGrid();
		}, [selectedEntities], actionsPerformer);
	}
	var performAddExistingEntity = this.performAddExistingEntity;

	this.performDelete = function(actionsPerformer) {
		if (currentState.selectedEntityId || getSelectedRowsCount() > 0) {
			if (window.confirm(translations.confirmDeleteMessage)) {
				blockGrid();
				mainController.callEvent("remove", elementPath, function() {
					unblockGrid();
				}, null, actionsPerformer);
			}
		} else {
			mainController.showMessage( {
				type : "error",
				content : translations.noRowSelectedError
			});
		}
	}
	var performDelete = this.performDelete;

	this.performCopy = function(actionsPerformer) {
		if (currentState.selectedEntityId || getSelectedRowsCount() > 0) {
			blockGrid();
			mainController.callEvent("copy", elementPath, function() {
				unblockGrid();
			}, null, actionsPerformer);
		} else {
			mainController.showMessage( {
				type : "error",
				content : translations.noRowSelectedError
			});
		}
	}
	var performCopy = this.performCopy;

	this.performActivate = function(actionsPerformer) {
		if (currentState.selectedEntityId || getSelectedRowsCount() > 0) {
			blockGrid();
			mainController.callEvent("activate", elementPath, function() {
				unblockGrid();
			}, null, actionsPerformer);
		} else {
			mainController.showMessage( {
				type : "error",
				content : translations.noRowSelectedError
			});
		}
	}
	var performActivate = this.performActivate;

	this.performDeactivate = function(actionsPerformer) {
		if (currentState.selectedEntityId || getSelectedRowsCount() > 0) {
			blockGrid();
			mainController.callEvent("deactivate", elementPath, function() {
				unblockGrid();
			}, null, actionsPerformer);
		} else {
			mainController.showMessage( {
				type : "error",
				content : translations.noRowSelectedError
			});
		}
	}
	var performDeactivate = this.performDeactivate;

	this.generateReportForEntity = function(actionsPerformer, arg1, args) {
		var selectedItems = new Array();
		for ( var i in currentState.selectedEntities) {
			if (currentState.selectedEntities[i]) {
				selectedItems.push(i);
			}
		}
		if (selectedItems.length > 0) {
			mainController.generateReportForEntity(actionsPerformer, arg1,
					args, selectedItems);
		} else {
			mainController.showMessage( {
				type : "error",
				content : translations.noRowSelectedError
			});
		}
	}

	this.performEvent = function(eventName, args, type) {
		this.fireEvent(null, eventName, args, type);
	}

	this.fireEvent = function(actionsPerformer, eventName, args, type) {
		blockGrid();
		mainController.callEvent(eventName, elementPath, function() {
			unblockGrid();
		}, args, actionsPerformer, type);
	}

	this.performLinkClicked = function(actionsPerformer) {
		if (currentState.selectedEntities) {
			var selectedEntitiesId = new Array();
			for ( var key in currentState.selectedEntities) {
				if (currentState.selectedEntities[key]) {
					selectedEntitiesId.push(key);
				}
			}
			if (selectedEntitiesId.length == 1) {
				linkClicked(selectedEntitiesId[0]);
			} else {
				linkClicked(selectedEntitiesId);
			}
			
			if (actionsPerformer) {
				actionsPerformer.performNext();
			}
		} else {
			mainController.showMessage( {
				type : "error",
				content : translations.noRowSelectedError
			});
		}
	}

	this.getLookupData = function(entityId) {
		var result = Object();
		result.entityId = entityId;
		result.lookupValue = hiddenColumnValues["lookupValue"][entityId];
		var lookupCodeLink = grid.getRowData(entityId).lookupCode;
		lookupCodeLink = lookupCodeLink.replace(/^<a[^>]*>/, "");
		lookupCodeLink = lookupCodeLink.replace(/<\/a>$/, "");
		result.lookupCode = lookupCodeLink;
		return result;
	}

	constructor(this);
}