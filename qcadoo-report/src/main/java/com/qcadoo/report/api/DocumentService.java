/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.4
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
package com.qcadoo.report.api;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;

import com.lowagie.text.DocumentException;
import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.model.api.Entity;

public abstract class DocumentService {

    private DecimalFormat decimalFormat;

    @Autowired
    private TranslationService translationService;

    public abstract void generateDocument(final Entity entity, final Locale locale) throws IOException, DocumentException;

    protected abstract String getSuffix();

    protected final TranslationService getTranslationService() {
        return translationService;
    }

    protected abstract String getReportTitle(final Locale locale);

    public final DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    protected void setDecimalFormat(final DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
    }

}
