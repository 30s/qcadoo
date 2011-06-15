/**
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
package com.qcadoo.report.api.pdf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.qcadoo.model.api.Entity;
import com.qcadoo.report.api.DocumentService;

public abstract class PdfDocumentService extends DocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfDocumentService.class);

    @Override
    public void generateDocument(final Entity entity, final Locale locale) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        try {
            setDecimalFormat((DecimalFormat) DecimalFormat.getInstance(locale));
            getDecimalFormat().setMaximumFractionDigits(3);
            getDecimalFormat().setMinimumFractionDigits(3);
            FileOutputStream fileOutputStream = new FileOutputStream((String) entity.getField("fileName") + getSuffix()
                    + PdfUtil.PDF_EXTENSION);
            PdfWriter writer = PdfWriter.getInstance(document, fileOutputStream);
            writer.setPageEvent(new PdfPageNumbering(
                    getTranslationService().translate("qcadooReport.commons.page.label", locale), getTranslationService()
                            .translate("qcadooReport.commons.of.label", locale)));
            document.setMargins(40, 40, 60, 60);
            buildPdfMetadata(document, locale);
            writer.createXmpMetadata();
            document.open();
            buildPdfContent(document, entity, locale);
            PdfUtil.addEndOfDocument(document, writer,
                    getTranslationService().translate("qcadooReport.commons.endOfPrint.label", locale));
            document.close();
        } catch (DocumentException e) {
            LOG.error("Problem with generating document - " + e.getMessage());
            document.close();
            throw e;
        }
    }

    protected void buildPdfMetadata(final Document document, final Locale locale) {
        document.addTitle(getReportTitle(locale));
        PdfUtil.addMetaData(document);
    }

    protected abstract void buildPdfContent(final Document document, final Entity entity, final Locale locale)
            throws DocumentException;

}
