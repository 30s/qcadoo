/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.0
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

package com.qcadoo.model.internal.search.restrictions;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;

import com.qcadoo.model.internal.search.Restriction;

public abstract class BaseRestriction implements Restriction {

    private final String fieldName;

    private final Object value;

    public BaseRestriction() {
        fieldName = null;
        value = null;
    }

    public BaseRestriction(final String fieldName, final Object value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    public final String getFieldName() {
        return fieldName;
    }

    public final Object getValue() {
        return value;
    }

    protected abstract Criterion getHibernateCriteria(String propertyName);

    @Override
    public final Criterion addToHibernateCriteria(final Criteria criteria) {
        String propertyName = fieldName;

        if (fieldName != null) {
            String[] path = fieldName.split("\\.");

            if (path.length > 1) {
                String alias = "";

                for (int i = 0; i < path.length - 1; i++) {
                    criteria.createAlias(alias + path[i], path[i] + "_" + i);
                    alias = path[i] + "_" + i + ".";
                }

                propertyName = alias + path[path.length - 1];
            }
        }

        return getHibernateCriteria(propertyName);
    }
}
