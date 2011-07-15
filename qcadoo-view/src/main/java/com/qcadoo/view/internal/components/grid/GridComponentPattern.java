/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.3
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
package com.qcadoo.view.internal.components.grid;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.qcadoo.model.api.FieldDefinition;
import com.qcadoo.model.api.types.EnumeratedType;
import com.qcadoo.model.api.types.HasManyType;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.internal.ComponentDefinition;
import com.qcadoo.view.internal.ComponentOption;
import com.qcadoo.view.internal.patterns.AbstractComponentPattern;
import com.qcadoo.view.internal.xml.ViewDefinitionParser;
import com.qcadoo.view.internal.xml.ViewDefinitionParserNodeException;

public final class GridComponentPattern extends AbstractComponentPattern {

    private static final String JSP_PATH = "elements/grid.jsp";

    private static final String JS_OBJECT = "QCD.components.elements.Grid";

    private static final int DEFAULT_GRID_HEIGHT = 300;

    private static final int DEFAULT_GRID_WIDTH = 300;

    private final Set<String> searchableColumns = new HashSet<String>();

    private final Set<String> orderableColumns = new HashSet<String>();

    private final Map<String, GridComponentColumn> columns = new LinkedHashMap<String, GridComponentColumn>();

    private FieldDefinition belongsToFieldDefinition;

    private String correspondingView;

    private String correspondingComponent;

    private boolean correspondingViewInModal = false;

    private boolean paginable = true;

    private boolean deletable = false;

    private boolean creatable = false;

    private boolean multiselect = false;

    private boolean hasPredefinedFilters = false;

    private final List<PredefinedFilter> predefinedFilters = new LinkedList<PredefinedFilter>();

    private int height = DEFAULT_GRID_HEIGHT;

    private int width = DEFAULT_GRID_WIDTH;

    private String defaultOrderColumn;

    private String defaultOrderDirection;

    private boolean lookup = false;

    private boolean activable = false;

    public GridComponentPattern(final ComponentDefinition componentDefinition) {
        super(componentDefinition);
    }

    @Override
    public ComponentState getComponentStateInstance() {
        return new GridComponentState(belongsToFieldDefinition, columns, defaultOrderColumn, defaultOrderDirection, activable);
    }

    @Override
    public String getJspFilePath() {
        return JSP_PATH;
    }

    @Override
    public String getJsFilePath() {
        return JS_PATH;
    }

    @Override
    public String getJsObjectName() {
        return JS_OBJECT;
    }

    @Override
    protected void initializeComponent() throws JSONException {
        getBelongsToFieldDefinition();
        parseOptions();

        activable = getDataDefinition().isActivable();

        if (correspondingView != null && correspondingComponent == null) {
            throwIllegalStateException("Missing correspondingComponent for grid");
        }
    }

    private void getBelongsToFieldDefinition() {
        if (getScopeFieldDefinition() != null) {
            if (HasManyType.class.isAssignableFrom(getScopeFieldDefinition().getType().getClass())) {
                HasManyType hasManyType = (HasManyType) getScopeFieldDefinition().getType();
                belongsToFieldDefinition = hasManyType.getDataDefinition().getField(hasManyType.getJoinFieldName());
            } else {
                throwIllegalStateException("Scope field for grid be a hasMany one");
            }
        }
    }

    @Override
    protected JSONObject getJsOptions(final Locale locale) throws JSONException {
        JSONObject json = super.getJsOptions(locale);
        json.put("paginable", paginable);
        json.put("deletable", deletable);
        json.put("creatable", creatable);
        json.put("multiselect", multiselect);
        json.put("activable", activable);

        json.put("hasPredefinedFilters", hasPredefinedFilters);

        JSONArray predefinedFiltersArray = new JSONArray();
        for (PredefinedFilter predefinedFilter : predefinedFilters) {
            predefinedFiltersArray.put(predefinedFilter.toJson());
        }

        json.put("predefinedFilters", predefinedFiltersArray);

        json.put("height", height);
        json.put("width", width);
        json.put("fullscreen", width == 0 || height == 0);
        json.put("lookup", lookup);
        json.put("correspondingView", correspondingView);
        json.put("correspondingComponent", correspondingComponent);
        json.put("correspondingViewInModal", correspondingViewInModal);
        json.put("prioritizable", getDataDefinition().isPrioritizable());
        json.put("searchableColumns", new JSONArray(searchableColumns));
        json.put("orderableColumns", new JSONArray(orderableColumns));

        if (belongsToFieldDefinition != null) {
            json.put("belongsToFieldName", belongsToFieldDefinition.getName());
        }

        json.put("columns", getColumnsForJsOptions(locale));

        JSONObject translations = new JSONObject();

        addTranslation(translations, "unactiveVisibleButton", locale);
        addTranslation(translations, "unactiveNotVisibleButton", locale);
        addTranslation(translations, "addFilterButton", locale);
        addTranslation(translations, "clearFilterButton", locale);
        addTranslation(translations, "noResults", locale);
        addTranslation(translations, "removeFilterButton", locale);
        addTranslation(translations, "newButton", locale);
        addTranslation(translations, "deleteButton", locale);
        addTranslation(translations, "upButton", locale);
        addTranslation(translations, "downButton", locale);
        addTranslation(translations, "perPage", locale);
        addTranslation(translations, "outOfPages", locale);
        addTranslation(translations, "noRowSelectedError", locale);
        addTranslation(translations, "confirmDeleteMessage", locale);
        addTranslation(translations, "wrongSearchCharacterError", locale);
        addTranslation(translations, "header", locale);
        addTranslation(translations, "selectAll", locale);
        addTranslation(translations, "diselectAll", locale);

        addTranslation(translations, "customPredefinedFilter", locale);
        for (PredefinedFilter filter : predefinedFilters) {
            addTranslation(translations, "filter." + filter.getName(), locale);
        }

        translations.put("loading", getTranslationService().translate("qcadooView.loading", locale));

        json.put("translations", translations);

        return json;
    }

    public void addColumn(final String name, final String fields, final String expression, final Boolean isLink,
            final Integer width, final boolean isOrderable, boolean isSearchable) {
        GridComponentColumn column = new GridComponentColumn(name);
        for (FieldDefinition field : parseFields(fields)) {
            column.addField(field);
        }
        column.setExpression(expression);
        if (isLink != null) {
            column.setLink(isLink);
        }
        if (width != null) {
            column.setWidth(width);
        }
        columns.put(name, column);
        if (isOrderable) {
            orderableColumns.add(name);
        }
        if (isSearchable) {
            searchableColumns.add(name);
        }
    }

    public void removeColumn(final String name) {
        columns.remove(name);
        orderableColumns.remove(name);
        searchableColumns.remove(name);
    }

    private void addTranslation(final JSONObject translation, final String key, final Locale locale) throws JSONException {
        translation.put(key, getTranslationService()
                .translate(getTranslationPath() + "." + key, "qcadooView.grid." + key, locale));
    }

    private JSONArray getColumnsForJsOptions(final Locale locale) throws JSONException {
        JSONArray jsonColumns = new JSONArray();
        String nameTranslation = null;
        for (GridComponentColumn column : columns.values()) {
            if (column.getFields().size() == 1) {
                String fieldCode = getDataDefinition().getPluginIdentifier() + "." + getDataDefinition().getName() + "."
                        + column.getFields().get(0).getName();
                nameTranslation = getTranslationService().translate(getTranslationPath() + ".column." + column.getName(),
                        fieldCode + ".label", locale);
            } else {
                nameTranslation = getTranslationService().translate(getTranslationPath() + ".column." + column.getName(), locale);
            }
            JSONObject jsonColumn = new JSONObject();
            jsonColumn.put("name", column.getName());
            jsonColumn.put("label", nameTranslation);
            jsonColumn.put("link", column.isLink());
            jsonColumn.put("hidden", column.isHidden());
            jsonColumn.put("width", column.getWidth());
            jsonColumn.put("align", column.getAlign());
            jsonColumn.put("filterValues", getFilterValuesForColumn(column, locale));
            jsonColumns.put(jsonColumn);
        }

        return jsonColumns;
    }

    public JSONObject getFilterValuesForColumn(final GridComponentColumn column, final Locale locale) throws JSONException {
        if (column.getFields().size() != 1) {
            return null;
        }

        JSONObject json = new JSONObject();

        if (column.getFields().get(0).getType() instanceof EnumeratedType) {
            EnumeratedType type = (EnumeratedType) column.getFields().get(0).getType();
            Map<String, String> sortedValues = type.values(locale);
            for (Map.Entry<String, String> value : sortedValues.entrySet()) {
                json.put(value.getKey(), value.getValue());
            }
        } else if (column.getFields().get(0).getType().getType().equals(Boolean.class)) {
            json.put("1", getTranslationService().translate("qcadooView.true", locale));
            json.put("0", getTranslationService().translate("qcadooView.false", locale));
        }

        if (json.length() > 0) {
            return json;
        } else {
            return null;
        }

    }

    @Override
    public void parse(final Node componentNode, final ViewDefinitionParser parser) throws ViewDefinitionParserNodeException {
        super.parse(componentNode, parser);
        NodeList childNodes = componentNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if ("predefinedFilters".equals(child.getNodeName())) {
                NodeList predefinedFilterChildNodes = child.getChildNodes();
                parsePredefinedFilterChildNodes(predefinedFilterChildNodes, parser);
                break;
            }
        }
    }

    private void parsePredefinedFilterChildNodes(final NodeList componentNodes, final ViewDefinitionParser parser)
            throws ViewDefinitionParserNodeException {
        for (int i = 0; i < componentNodes.getLength(); i++) {
            Node child = componentNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if ("predefinedFilter".equals(child.getNodeName())) {
                    PredefinedFilter predefinedFilter = new PredefinedFilter();
                    predefinedFilter.setName(parser.getStringAttribute(child, "name"));

                    NodeList restrictionNodes = child.getChildNodes();
                    for (int restrictionNodesIndex = 0; restrictionNodesIndex < restrictionNodes.getLength(); restrictionNodesIndex++) {
                        Node restrictionNode = restrictionNodes.item(restrictionNodesIndex);
                        if (restrictionNode.getNodeType() == Node.ELEMENT_NODE) {
                            if ("filterRestriction".equals(restrictionNode.getNodeName())) {
                                predefinedFilter.addFilterRestriction(parser.getStringAttribute(restrictionNode, "column"),
                                        parser.getStringAttribute(restrictionNode, "value"));
                            } else if ("filterOrder".equals(restrictionNode.getNodeName())) {
                                String column = parser.getStringAttribute(restrictionNode, "column");
                                String direction = parser.getStringAttribute(restrictionNode, "direction");
                                if (column == null) {
                                    throw new ViewDefinitionParserNodeException(restrictionNode,
                                            "'filterOrder' tag must contain 'collumn' attribute");
                                }
                                if (direction == null) {
                                    direction = "asc";
                                } else {
                                    if (!("asc".equals(direction) || "desc".equals(direction))) {
                                        throw new ViewDefinitionParserNodeException(restrictionNode, "unknown order direction: "
                                                + direction);
                                    }
                                }
                                predefinedFilter.setOrderColumn(column);
                                predefinedFilter.setOrderDirection(direction);
                            } else {
                                throw new ViewDefinitionParserNodeException(restrictionNode,
                                        "predefinedFilter can only contain 'filterRestriction' or 'filterOrder' tag");
                            }
                        }
                    }

                    if (predefinedFilter.getOrderColumn() == null && defaultOrderColumn != null) {
                        predefinedFilter.setOrderColumn(defaultOrderColumn);
                        predefinedFilter.setOrderDirection(defaultOrderDirection);
                    }

                    predefinedFilters.add(predefinedFilter);
                } else {
                    throwIllegalStateException("predefinedFilters can only contain 'predefinedFilter' tag");
                    throw new ViewDefinitionParserNodeException(child,
                            "predefinedFilters can only contain 'predefinedFilter' tag");
                }
            }
        }
    }

    private void parseOptions() {
        for (ComponentOption option : getOptions()) {
            if ("correspondingView".equals(option.getType())) {
                correspondingView = option.getValue();
            } else if ("correspondingComponent".equals(option.getType())) {
                correspondingComponent = option.getValue();
            } else if ("correspondingViewInModal".equals(option.getType())) {
                correspondingViewInModal = Boolean.parseBoolean(option.getValue());
            } else if ("paginable".equals(option.getType())) {
                paginable = Boolean.parseBoolean(option.getValue());
            } else if ("creatable".equals(option.getType())) {
                creatable = Boolean.parseBoolean(option.getValue());
            } else if ("multiselect".equals(option.getType())) {
                multiselect = Boolean.parseBoolean(option.getValue());
            } else if ("hasPredefinedFilters".equals(option.getType())) {
                hasPredefinedFilters = Boolean.parseBoolean(option.getValue());
            } else if ("deletable".equals(option.getType())) {
                deletable = Boolean.parseBoolean(option.getValue());
            } else if ("height".equals(option.getType())) {
                height = Integer.parseInt(option.getValue());
            } else if ("width".equals(option.getType())) {
                width = Integer.parseInt(option.getValue());
            } else if ("fullscreen".equals(option.getType())) {
                width = 0;
                height = 0;
            } else if ("lookup".equals(option.getType())) {
                lookup = Boolean.parseBoolean(option.getValue());
            } else if ("searchable".equals(option.getType())) {
                searchableColumns.addAll(parseColumns(option.getValue()));
            } else if ("orderable".equals(option.getType())) {
                orderableColumns.addAll(parseColumns(option.getValue()));
            } else if ("order".equals(option.getType())) {
                defaultOrderColumn = option.getAtrributeValue("column");
                defaultOrderDirection = option.getAtrributeValue("direction");
                if (predefinedFilters != null) {
                    for (PredefinedFilter predefinedFilter : predefinedFilters) {
                        if (predefinedFilter.getOrderColumn() == null) {
                            predefinedFilter.setOrderColumn(defaultOrderColumn);
                            predefinedFilter.setOrderDirection(defaultOrderDirection);
                        }
                    }
                }
            } else if ("column".equals(option.getType())) {
                parseColumnOption(option);
            }
        }
        if (defaultOrderColumn == null) {
            throwIllegalStateException("grid must contain 'order' option");
        }
    }

    private void parseColumnOption(final ComponentOption option) {
        GridComponentColumn column = new GridComponentColumn(option.getAtrributeValue("name"));
        String fields = option.getAtrributeValue("fields");
        if (fields != null) {
            for (FieldDefinition field : parseFields(fields)) {
                column.addField(field);
            }
        }
        column.setExpression(option.getAtrributeValue("expression"));
        String columnWidth = option.getAtrributeValue("width");
        if (columnWidth != null) {
            column.setWidth(Integer.valueOf(columnWidth));
        }
        if (option.getAtrributeValue("link") != null) {
            column.setLink(Boolean.parseBoolean(option.getAtrributeValue("link")));
        }
        if (option.getAtrributeValue("hidden") != null) {
            column.setHidden(Boolean.parseBoolean(option.getAtrributeValue("hidden")));
        }
        columns.put(column.getName(), column);
    }

    private Set<String> parseColumns(final String columns) {
        Set<String> set = new HashSet<String>();
        for (String column : columns.split("\\s*,\\s*")) {
            set.add(column);
        }
        return set;
    }

    private Set<FieldDefinition> parseFields(final String fields) {
        Set<FieldDefinition> set = new HashSet<FieldDefinition>();
        for (String field : fields.split("\\s*,\\s*")) {
            set.add(getDataDefinition().getField(field));
        }
        return set;
    }

    private void throwIllegalStateException(final String message) {
        throw new IllegalStateException(getViewDefinition().getPluginIdentifier() + "." + getViewDefinition().getName() + "#"
                + getPath() + ": " + message);
    }
}
