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
package com.qcadoo.view.api.exception;

public class ExceptionInfo {

    private final String messageHeader;

    private final String messageExplanation;

    private final String[] messageExplanationArgs;

    public ExceptionInfo(final String messageHeader, final String messageExplanation, final String... messageExplanationArgs) {
        this.messageHeader = messageHeader;
        this.messageExplanation = messageExplanation;
        this.messageExplanationArgs = messageExplanationArgs;
    }

    public String getMessageHeader() {
        return messageHeader;
    }

    public String getMessageExplanation() {
        return messageExplanation;
    }

    public String[] getMessageExplanationArgs() {
        return messageExplanationArgs;
    }

}
