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

package com.qcadoo.mes.model.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.qcadoo.mes.internal.DataAccessTest;
import com.qcadoo.model.api.search.Restrictions;
import com.qcadoo.model.api.search.SearchCriteria;

public final class SearchCriteriaBuilderTest extends DataAccessTest {

    @Test
    public void shouldCreateCriteriaWithDefaults() throws Exception {
        // when
        SearchCriteria searchCriteria = (SearchCriteria) dataDefinition.find();

        // then
        assertEquals(0, searchCriteria.getFirstResult());
        assertEquals(Integer.MAX_VALUE, searchCriteria.getMaxResults());
        assertEquals(dataDefinition, searchCriteria.getDataDefinition());
        assertEquals("id", searchCriteria.getOrder().getFieldName());
        assertTrue(searchCriteria.getOrder().isAsc());
        assertTrue(searchCriteria.getRestrictions().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrownAnExceptionIfThereIsTooManyRestrictions() throws Exception {
        // when
        dataDefinition.find().addRestriction(Restrictions.eq(fieldDefinitionAge, 5))
                .addRestriction(Restrictions.eq(fieldDefinitionName, "asb%"))
                .addRestriction(Restrictions.eq(fieldDefinitionName, "asd%"))
                .addRestriction(Restrictions.eq(fieldDefinitionName, "asw%"))
                .addRestriction(Restrictions.eq(fieldDefinitionName, "asg%"))
                .addRestriction(Restrictions.eq(fieldDefinitionName, "asu%"));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrownAnExceptionIfOrderIsNull() throws Exception {
        // when
        dataDefinition.find().setOrderAscBy(null);
    }

}
