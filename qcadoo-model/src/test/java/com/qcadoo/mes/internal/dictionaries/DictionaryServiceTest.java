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

package com.qcadoo.mes.internal.dictionaries;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.qcadoo.model.api.DictionaryService;
import com.qcadoo.model.beans.qcadooModel.QcadooModelDictionary;
import com.qcadoo.model.beans.qcadooModel.QcadooModelDictionaryItem;
import com.qcadoo.model.internal.dictionaries.DictionaryServiceImpl;

public class DictionaryServiceTest {

    private final SessionFactory sessionFactory = mock(SessionFactory.class, RETURNS_DEEP_STUBS);

    private DictionaryService dictionaryService = null;

    @Before
    public void init() {
        dictionaryService = new DictionaryServiceImpl();
        ReflectionTestUtils.setField(dictionaryService, "sessionFactory", sessionFactory);
    }

    @Test
    public void shouldReturnListOfDictionaries() throws Exception {
        // given
        QcadooModelDictionary dict1 = new QcadooModelDictionary();
        dict1.setName("Dict1");
        QcadooModelDictionary dict2 = new QcadooModelDictionary();
        dict2.setName("Dict2");
        QcadooModelDictionary dict3 = new QcadooModelDictionary();
        dict3.setName("Dict3");

        given(sessionFactory.getCurrentSession().createQuery("from Dictionary").list()).willReturn(
                newArrayList(dict1, dict2, dict3));

        // when
        Set<String> dictionaries = dictionaryService.dictionaries();

        // then
        assertThat(dictionaries.size(), equalTo(3));
        assertThat(dictionaries, hasItems("Dict1", "Dict2", "Dict3"));
    }

    @Test
    public void shouldReturnSortedListOfDictionaryValues() throws Exception {
        // given
        QcadooModelDictionaryItem item1 = new QcadooModelDictionaryItem();
        item1.setName("aaa");
        QcadooModelDictionaryItem item2 = new QcadooModelDictionaryItem();
        item2.setName("ccc");
        QcadooModelDictionaryItem item3 = new QcadooModelDictionaryItem();
        item3.setName("bbb");

        given(
                sessionFactory.getCurrentSession().createCriteria(QcadooModelDictionaryItem.class)
                        .createAlias("dictionary", "dc").add(Mockito.any(Criterion.class)).addOrder(Mockito.any(Order.class))
                        .list()).willReturn(newArrayList(item1, item3, item2));

        // when
        Map<String, String> values = dictionaryService.values("dict", Locale.ENGLISH);

        // then
        assertThat(values.size(), equalTo(3));
        assertThat(values.get("aaa"), equalTo("aaa"));
        assertThat(values.get("bbb"), equalTo("bbb"));
        assertThat(values.get("ccc"), equalTo("ccc"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrownAnExceptionIfDictionaryNameIsNull() throws Exception {
        // when
        dictionaryService.values(null, Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrownAnExceptionIfDictionaryNameIsEmpty() throws Exception {
        // when
        dictionaryService.values("", Locale.ENGLISH);
    }

}
