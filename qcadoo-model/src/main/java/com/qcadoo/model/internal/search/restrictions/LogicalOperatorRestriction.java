/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.1
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.qcadoo.model.internal.search.Restriction;
import com.qcadoo.model.internal.search.RestrictionLogicalOperator;

public class LogicalOperatorRestriction implements Restriction {

    private final RestrictionLogicalOperator operator;

    private final Restriction[] restrictions;

    public LogicalOperatorRestriction(final RestrictionLogicalOperator operator, final Restriction... restrictions) {
        checkNotNull(operator);
        checkNotNull(restrictions);
        checkArgument(restrictions.length > 0);
        this.operator = operator;
        this.restrictions = restrictions;
    }

    @Override
    public Criterion addToHibernateCriteria(final Criteria criteria) {
        switch (operator) {
            case NOT:
                checkState(restrictions.length == 1, "not can only have one argument");
                return Restrictions.not(restrictions[0].addToHibernateCriteria(criteria));
            case AND:
                return createAndRestriction(criteria, Arrays.asList(restrictions));
            case OR:
                return createOrRestriction(criteria, Arrays.asList(restrictions));
            default:
                throw new IllegalArgumentException("Unknown restriction operator");
        }
    }

    private Criterion createAndRestriction(final Criteria criteria, final List<Restriction> innerRestrictions) {
        if (innerRestrictions.size() == 1) {
            return innerRestrictions.get(0).addToHibernateCriteria(criteria);
        } else {
            Criterion first = innerRestrictions.get(0).addToHibernateCriteria(criteria);
            return Restrictions
                    .and(first, createAndRestriction(criteria, innerRestrictions.subList(1, innerRestrictions.size())));
        }
    }

    private Criterion createOrRestriction(final Criteria criteria, final List<Restriction> innerRestrictions) {
        if (innerRestrictions.size() == 1) {
            return innerRestrictions.get(0).addToHibernateCriteria(criteria);
        } else {
            Criterion first = innerRestrictions.get(0).addToHibernateCriteria(criteria);
            return Restrictions.or(first, createOrRestriction(criteria, innerRestrictions.subList(1, innerRestrictions.size())));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + Arrays.hashCode(restrictions);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LogicalOperatorRestriction other = (LogicalOperatorRestriction) obj;
        if (operator != other.operator) {
            return false;
        }
        if (!Arrays.equals(restrictions, other.restrictions)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return operator.getValue() + "(" + Arrays.toString(restrictions) + ")";
    }

}
