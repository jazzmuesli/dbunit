/*
 * CompositeOperation.java   Feb 18, 2002
 *
 * The DbUnit Database Testing Framework
 * Copyright (C)2002, Manuel Laflamme
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.dbunit.operation;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;

import java.sql.SQLException;

/**
 * Truncate tables present in the specified dataset. If the dataset does not
 * contains a particular table, but that table exists in the database,
 * the database table is not affected. Table are truncated in
 * reverse sequence.
 * <p>
 * This operation has the same effect of as {@link DeleteAllOperation}.
 * TruncateTableOperation is faster, and it is non-logged, meaning it cannot be
 * rollback. DeleteAllOperation is more portable because not all database vendor
 * support TRUNCATE_TABLE TABLE statement.
 *
 * @author Manuel Laflamme
 * @since Apr 10, 2003
 * @version $Revision$
 * @see DeleteAllOperation
 */
public class TruncateTableOperation extends DeleteAllOperation
{
    static final String SUPPORT_BATCH_STATEMENT = "dbunit.database.supportBatchStatement";

    TruncateTableOperation()
    {
    }

    ////////////////////////////////////////////////////////////////////////////
    // DeleteAllOperation class

    protected String getDeleteAllCommand()
    {
        return "truncate table ";
    }

    ////////////////////////////////////////////////////////////////////////////
    // DatabaseOperation class

    public void execute(IDatabaseConnection connection, IDataSet dataSet)
            throws DatabaseUnitException, SQLException
    {
        // Patch to make it work with MS SQL Server
        String oldValue = System.getProperty(SUPPORT_BATCH_STATEMENT, "true");
        try
        {
            System.setProperty(SUPPORT_BATCH_STATEMENT, "false");
            super.execute(connection, dataSet);
        }
        finally
        {
            System.setProperty(SUPPORT_BATCH_STATEMENT, oldValue);
        }
    }
}






