/*
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
package org.dbunit.database;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.filter.SequenceTableFilter;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This filter orders tables using dependency information provided by
 * {@link java.sql.DatabaseMetaData#getExportedKeys}.
 *
 * @author Manuel Laflamme
 * @since Mar 23, 2003
 * @version $Revision$
 */
public class DatabaseSequenceFilter extends SequenceTableFilter
{
    /**
     * Create a DatabaseSequenceFilter that only exposes specified table names.
     */
    public DatabaseSequenceFilter(IDatabaseConnection connection,
            String[] tableNames) throws DataSetException, SQLException
    {
        super(sortTableNames(connection, tableNames));
    }

    /**
     * Create a DatabaseSequenceFilter that exposes all the database tables.
     */
    public DatabaseSequenceFilter(IDatabaseConnection connection)
            throws DataSetException, SQLException
    {
        this(connection, connection.createDataSet().getTableNames());
    }

    private static String[] sortTableNames(IDatabaseConnection connection,
            String[] tableNames) throws DataSetException, SQLException
    {
        Arrays.sort((String[])tableNames.clone(),
                new TableSequenceComparator(connection));
        return tableNames;
    }

    private static class TableSequenceComparator implements Comparator
    {
        IDatabaseConnection _connection;
        Map _dependentMap = new HashMap();

        public TableSequenceComparator(IDatabaseConnection connection)
        {
            _connection = connection;
        }

        public int compare(Object o1, Object o2)
        {
            String tableName1 = (String)o1;
            String tableName2 = (String)o2;

            try
            {
                if (getDependentTableNames(tableName1).contains(tableName2))
                {
                    if (getDependentTableNames(tableName2).contains(tableName1))
                    {
                        throw new CyclicTablesDependencyException(tableName1, tableName2);
                    }
                    return -1;
                }
            }
            catch (SQLException e)
            {

            }
            catch (CyclicTablesDependencyException e)
            {

            }

            return tableName1.compareTo(tableName2);
        }

        private Set getDependentTableNames(String tableName)
                throws SQLException
        {
            if (_dependentMap.containsKey(tableName))
            {
                return (Set)_dependentMap.get(tableName);
            }

            DatabaseMetaData metaData = _connection.getConnection().getMetaData();
            String schema = _connection.getSchema();

            ResultSet resultSet = metaData.getExportedKeys(null, schema, tableName);
            try
            {
                Set foreignTableSet = new HashSet();

                while (resultSet.next())
                {
                    // TODO : add support for qualified table names
//                    String foreignSchemaName = resultSet.getString(6);
                    String foreignTableName = resultSet.getString(7);

                    foreignTableSet.add(foreignTableName);
                }

                _dependentMap.put(tableName, foreignTableSet);
                return foreignTableSet;
            }
            finally
            {
                resultSet.close();
            }
        }

    }

}