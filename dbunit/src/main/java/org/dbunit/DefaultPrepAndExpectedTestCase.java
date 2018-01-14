/*
 *
 * The DbUnit Database Testing Framework
 * Copyright (C)2002-2008, DbUnit.org
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
package org.dbunit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.SortedTable;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.util.fileloader.DataFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case base class supporting prep data and expected data. Prep data is the
 * data needed for the test to run. Expected data is the data needed to compare
 * if the test ran successfully.
 *
 * Use this class in two ways:
 * <ol>
 * <li>Dependency inject it as its interface into a test class.</li>
 * <p>
 * Configure a bean of its interface, injecting a IDatabaseTester and a
 * DataFileLoader using the databaseTester and a dataFileLoader properties.
 * </p>
 *
 * <li>Extend it in a test class.</li>
 * <p>
 * Obtain IDatabaseTester and DataFileLoader instances (possibly dependency
 * injecting them into the test class) and set them accordingly, probably in a
 * setup type of method, such as:
 *
 * <pre>
 * &#064;Before
 * public void setDbunitTestDependencies()
 * {
 *     setDatabaseTester(databaseTester);
 *     setDataFileLoader(dataFileLoader);
 * }
 * </pre>
 *
 * </p>
 * </ol>
 *
 * To setup, execute, and clean up tests, call the configureTest(), preTest(),
 * and postTest() methods. Note there is a preTest() convenience method that
 * takes the same parameters as the configureTest() method; use it instead of
 * using both configureTest() and preTest().
 *
 * Where the test case calls them depends on data needs:
 * <ul>
 * <li>For the whole test case, i.e. in setUp() and tearDown() or &#064;Before
 * and &#064;After.</li>
 * <li>In each test method.</li>
 * <li>Or some combination of both test case setup/teardown and test methods.
 * </li>
 * </ul>
 *
 * <h4>When each test method requires different prep and expected data</h4>
 *
 * If each test method requires its own prep and expected data, then the test
 * methods will look something like the following:
 *
 * <pre>
 * &#064;Autowired
 * private PrepAndExpectedTestCase tc;
 *
 * &#064;Test
 * public void testExample() throws Exception
 * {
 *     try
 *     {
 *         final String[] prepDataFiles = {}; // define prep files
 *         final String[] expectedDataFiles = {}; // define expected files
 *         final VerifyTableDefinition[] tables = {}; // define tables to verify
 *
 *         tc.preTest(tables, prepDataFiles, expectedDataFiles);
 *
 *         // execute test steps
 *     } catch (Exception e)
 *     {
 *         log.error(&quot;Test error&quot;, e);
 *         throw e;
 *     } finally
 *     {
 *         tc.postTest();
 *     }
 * }
 * </pre>
 *
 * <h4>When all test methods share the same prep and/or expected data</h4>
 *
 * If each test method can share all of the prep and/or expected data, then use
 * setUp() for the configureTest() and preTest() calls and tearDown() for the
 * postTest() call. The methods will look something like the following:
 *
 * <pre>
 * &#064;Override
 * protected void setUp() throws Exception
 * {
 *     setDatabaseTester(databaseTester);
 *     setDataFileLoader(dataFileLoader);
 *
 *     String[] prepDataFiles = {}; // define prep files
 *     String[] expectedDataFiles = {}; // define expected files
 *     VerifyTableDefinition[] tables = {}; // define tables to verify
 *
 *     preTest(tables, prepDataFiles, expectedDataFiles);
 *
 *     // call this if overriding setUp() and databaseTester &amp; dataFileLoader
 *     // are already set.
 *     super.setUp();
 * }
 *
 * &#064;Override
 * protected void tearDown() throws Exception
 * {
 *     postTest();
 *     super.tearDown();
 * }
 *
 * &#064;Test
 * public void testExample() throws Exception
 * {
 *     // execute test steps
 * }
 * </pre>
 *
 * Note that it is unlikely that all test methods can share the same expected
 * data.
 *
 * <h4>Sharing common (but not all) prep or expected data among test methods.
 * </h4>
 *
 * Put common data in one or more files and pass the needed ones in the correct
 * data file array.
 *
 * <h4>Java 8 and Anonymous Interfaces</h4>
 * <p>
 * Release 2.5.2 introduced interface {@link PrepAndExpectedTestCaseSteps} and
 * the
 * {@link #runTest(VerifyTableDefinition[], String[], String[], PrepAndExpectedTestCaseSteps)}
 * method. This allows for encapsulating test steps into an anonymous inner
 * class or a Java 8+ lambda and avoiding the try/catch/finally template in
 * tests.
 * </p>
 *
 * <pre>
 * &#064;Autowired
 * private PrepAndExpectedTestCase tc;
 *
 * &#064;Test
 * public void testExample() throws Exception
 * {
 *     final String[] prepDataFiles = {}; // define prep files
 *     final String[] expectedDataFiles = {}; // define expected files
 *     final VerifyTableDefinition[] tables = {}; // define tables to verify
 *     final PrepAndExpectedTestCaseSteps testSteps = () -> {
 *         // execute test steps
 *
 *         return null; // or an object for use outside the Steps
 *     };
 *
 *     tc.runTest(tables, prepDataFiles, expectedDataFiles, testSteps);
 * }
 * </pre>
 *
 * <h4>Notes</h4>
 * <ol>
 * <li>For additional examples, refer to the ITs (listed in the See Also
 * section).</li>
 * <li>To change the setup or teardown operation (e.g. change the teardown to
 * org.dbunit.operation.DatabaseOperation.DELETE_ALL), set the setUpOperation or
 * tearDownOperation property on the databaseTester.</li>
 * <li>To set DatabaseConfig features/properties, one way is to extend this
 * class and override the setUpDatabaseConfig(DatabaseConfig config) method from
 * DatabaseTestCase.</li>
 * </ol>
 *
 * @see org.dbunit.DefaultPrepAndExpectedTestCaseDiIT
 * @see org.dbunit.DefaultPrepAndExpectedTestCaseExtIT
 *
 * @author Jeff Jensen jeffjensen AT users.sourceforge.net
 * @author Last changed by: $Author$
 * @version $Revision$ $Date$
 * @since 2.4.8
 */
public class DefaultPrepAndExpectedTestCase extends DBTestCase
        implements PrepAndExpectedTestCase
{
    private final Logger log =
            LoggerFactory.getLogger(DefaultPrepAndExpectedTestCase.class);

    private static final String DATABASE_TESTER_IS_NULL_MSG =
            "databaseTester is null; must configure or set it first";

    public static final String TEST_ERROR_MSG = "DbUnit test error.";

    private IDatabaseTester databaseTester;
    private DataFileLoader dataFileLoader;

    private IDataSet prepDataSet = new DefaultDataSet();
    private IDataSet expectedDataSet = new DefaultDataSet();
    private VerifyTableDefinition[] verifyTableDefs = {};

    private ExpectedDataSetAndVerifyTableDefinitionVerifier expectedDataSetAndVerifyTableDefinitionVerifier =
            new DefaultExpectedDataSetAndVerifyTableDefinitionVerifier();

    /** Create new instance. */
    public DefaultPrepAndExpectedTestCase()
    {
    }

    /**
     * Create new instance with specified dataFileLoader and databaseTester.
     *
     * @param dataFileLoader
     *            Load to use for loading the data files.
     * @param databaseTester
     *            Tester to use for database manipulation.
     */
    public DefaultPrepAndExpectedTestCase(final DataFileLoader dataFileLoader,
            final IDatabaseTester databaseTester)
    {
        this.dataFileLoader = dataFileLoader;
        this.databaseTester = databaseTester;
    }

    /**
     * Create new instance with specified test case name.
     *
     * @param name
     *            The test case name.
     */
    public DefaultPrepAndExpectedTestCase(final String name)
    {
        super(name);
    }

    /**
     * {@inheritDoc} This implementation returns the databaseTester set by the
     * test.
     */
    @Override
    public IDatabaseTester newDatabaseTester() throws Exception
    {
        // questionable, but there is not a "setter" for any parent...
        return databaseTester;
    }

    /**
     * {@inheritDoc} Returns the prep dataset.
     */
    @Override
    public IDataSet getDataSet() throws Exception
    {
        return prepDataSet;
    }

    /**
     * {@inheritDoc}
     */
    public void configureTest(
            final VerifyTableDefinition[] verifyTableDefinitions,
            final String[] prepDataFiles, final String[] expectedDataFiles)
            throws Exception
    {
        log.debug("configureTest: saving instance variables");
        this.prepDataSet = makeCompositeDataSet(prepDataFiles, "prep");
        this.expectedDataSet =
                makeCompositeDataSet(expectedDataFiles, "expected");
        this.verifyTableDefs = verifyTableDefinitions;
    }

    /**
     * {@inheritDoc}
     */
    public void preTest() throws Exception
    {
        setupData();
    }

    /**
     * {@inheritDoc}
     */
    public void preTest(final VerifyTableDefinition[] tables,
            final String[] prepDataFiles, final String[] expectedDataFiles)
            throws Exception
    {
        configureTest(tables, prepDataFiles, expectedDataFiles);
        preTest();
    }

    /**
     * {@inheritDoc}
     */
    public Object runTest(final VerifyTableDefinition[] verifyTables,
            final String[] prepDataFiles, final String[] expectedDataFiles,
            final PrepAndExpectedTestCaseSteps testSteps) throws Exception
    {
        final Object result;

        try
        {
            preTest(verifyTables, prepDataFiles, expectedDataFiles);
            result = testSteps.run();
        } catch (final Exception e)
        {
            log.error(TEST_ERROR_MSG, e);
            // don't verify table data when test execution has errors as:
            // * a verify data failure masks the test error exception
            // * tables in unknown state and therefore probably not accurate
            postTest(false);
            throw e;
        }

        postTest();

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void postTest() throws Exception
    {
        postTest(true);
    }

    /**
     * {@inheritDoc}
     */
    public void postTest(final boolean verifyData) throws Exception
    {
        try
        {
            if (verifyData)
            {
                verifyData();
            }
        } finally
        {
            // it is deliberate to have cleanup exceptions shadow verify
            // failures so user knows db is probably in unknown state (for
            // those not using an in-memory db or transaction rollback),
            // otherwise would mask probable cause of subsequent test failures
            cleanupData();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cleanupData() throws Exception
    {
        try
        {
            final IDataSet dataset =
                    new CompositeDataSet(prepDataSet, expectedDataSet);
            final String[] tableNames = dataset.getTableNames();
            final int count = tableNames.length;
            log.info("cleanupData: about to clean up {} tables={}", count,
                    tableNames);

            if (databaseTester == null)
            {
                throw new IllegalStateException(DATABASE_TESTER_IS_NULL_MSG);
            }

            databaseTester.setTearDownOperation(getTearDownOperation());
            databaseTester.setDataSet(dataset);
            databaseTester.setOperationListener(getOperationListener());
            databaseTester.onTearDown();
            log.debug("cleanupData: Clean up done");
        } catch (final Exception e)
        {
            log.error("cleanupData: Exception:", e);
            throw e;
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        // parent tearDown() only cleans up prep data
        cleanupData();
        super.tearDown();
    }

    /**
     * Use the provided databaseTester to prep the database with the provided
     * prep dataset. See {@link org.dbunit.IDatabaseTester#onSetup()}.
     *
     * @throws Exception
     */
    public void setupData() throws Exception
    {
        log.debug("setupData: setting prep dataset and inserting rows");
        if (databaseTester == null)
        {
            throw new IllegalStateException(DATABASE_TESTER_IS_NULL_MSG);
        }

        try
        {
            super.setUp();
        } catch (final Exception e)
        {
            log.error("setupData: Exception with setting up data:", e);
            throw e;
        }
    }

    @Override
    protected DatabaseOperation getSetUpOperation() throws Exception
    {
        assertNotNull(DATABASE_TESTER_IS_NULL_MSG, databaseTester);
        return databaseTester.getSetUpOperation();
    }

    @Override
    protected DatabaseOperation getTearDownOperation() throws Exception
    {
        assertNotNull(DATABASE_TESTER_IS_NULL_MSG, databaseTester);
        return databaseTester.getTearDownOperation();
    }

    /**
     * {@inheritDoc} Uses the connection from the provided databaseTester.
     */
    public void verifyData() throws Exception
    {
        if (databaseTester == null)
        {
            throw new IllegalStateException(DATABASE_TESTER_IS_NULL_MSG);
        }

        final IDatabaseConnection connection = getConnection();

        final DatabaseConfig config = connection.getConfig();
        expectedDataSetAndVerifyTableDefinitionVerifier.verify(verifyTableDefs,
                expectedDataSet, config);

        try
        {
            final int tableDefsCount = verifyTableDefs.length;
            log.info(
                    "verifyData: about to verify {} tables"
                            + " using verifyTableDefinitions={}",
                    tableDefsCount, verifyTableDefs);
            if (tableDefsCount == 0)
            {
                log.warn("verifyData: No tables to verify as"
                        + " no VerifyTableDefinitions specified");
            }

            for (int i = 0; i < tableDefsCount; i++)
            {
                final VerifyTableDefinition td = verifyTableDefs[i];
                final String[] excludeColumns = td.getColumnExclusionFilters();
                final String[] includeColumns = td.getColumnInclusionFilters();
                final String tableName = td.getTableName();

                log.info("verifyData: Verifying table '{}'", tableName);
                final ITable expectedTable =
                        loadTableDataFromDataSet(tableName);
                final ITable actualTable =
                        loadTableDataFromDatabase(tableName, connection);

                verifyData(expectedTable, actualTable, excludeColumns,
                        includeColumns);
            }
        } catch (final Exception e)
        {
            log.error("verifyData: Exception:", e);
            throw e;
        } finally
        {
            log.debug("verifyData: Verification done, closing connection");
            connection.close();
        }
    }

    public ITable loadTableDataFromDataSet(final String tableName)
            throws DataSetException
    {
        ITable table = null;

        final String methodName = "loadTableDataFromDataSet";

        log.debug("{}: Loading table {} from expected dataset", methodName,
                tableName);
        try
        {
            table = expectedDataSet.getTable(tableName);
        } catch (final Exception e)
        {
            final String msg = methodName + ": Problem obtaining table '"
                    + tableName + "' from expected dataset";
            log.error(msg, e);
            throw new DataSetException(msg, e);
        }
        return table;
    }

    public ITable loadTableDataFromDatabase(final String tableName,
            final IDatabaseConnection connection) throws Exception
    {
        ITable table = null;

        final String methodName = "loadTableDataFromDatabase";

        log.debug("{}: Loading table {} from database", methodName, tableName);
        try
        {
            table = connection.createTable(tableName);
        } catch (final Exception e)
        {
            final String msg = methodName + ": Problem obtaining table '"
                    + tableName + "' from database";
            log.error(msg, e);
            throw new DataSetException(msg, e);
        }
        return table;
    }

    /**
     * For the specified expected and actual tables (and excluding and including
     * the specified columns), verify the actual data is as expected.
     *
     * @param expectedTable
     *            The expected table to compare the actual table to.
     * @param actualTable
     *            The actual table to compare to the expected table.
     * @param excludeColumns
     *            The column names to exclude from comparison. See
     *            {@link org.dbunit.dataset.filter.DefaultColumnFilter#excludeColumn(String)}
     *            .
     * @param includeColumns
     *            The column names to only include in comparison. See
     *            {@link org.dbunit.dataset.filter.DefaultColumnFilter#includeColumn(String)}
     *            .
     * @throws DatabaseUnitException
     */
    public void verifyData(final ITable expectedTable, final ITable actualTable,
            final String[] excludeColumns, final String[] includeColumns)
            throws DatabaseUnitException
    {
        final String methodName = "verifyData";
        // Filter out the columns from the expected and actual results
        log.debug("{}: Applying filters to expected table", methodName);
        final ITable expectedFilteredTable = applyColumnFilters(expectedTable,
                excludeColumns, includeColumns);
        log.debug("{}: Applying filters to actual table", methodName);
        final ITable actualFilteredTable =
                applyColumnFilters(actualTable, excludeColumns, includeColumns);

        log.debug("{}: Sorting expected table", methodName);
        final SortedTable expectedSortedTable =
                new SortedTable(expectedFilteredTable);
        log.debug("{}: Sorted expected table={}", methodName,
                expectedSortedTable);

        log.debug("{}: Sorting actual table", methodName);
        final SortedTable actualSortedTable = new SortedTable(
                actualFilteredTable, expectedFilteredTable.getTableMetaData());
        log.debug("{}: Sorted actual table={}", methodName, actualSortedTable);

        log.debug("{}: Creating additionalColumnInfo", methodName);
        final Column[] additionalColumnInfo =
                makeAdditionalColumnInfo(expectedTable, excludeColumns);
        log.debug("{}: additionalColumnInfo={}", methodName,
                additionalColumnInfo);

        log.debug("{}: Comparing expected table to actual table", methodName);
        Assertion.assertEquals(expectedSortedTable, actualSortedTable,
                additionalColumnInfo);
    }

    /**
     * Don't add excluded columns to additionalColumnInfo as they are not found
     * and generate a not found message in the fail message.
     */
    protected Column[] makeAdditionalColumnInfo(final ITable expectedTable,
            final String[] excludeColumns) throws DataSetException
    {
        final List<Column> keepColumnsList = new ArrayList<Column>();
        final List<String> excludeColumnsList = Arrays.asList(excludeColumns);

        final Column[] allColumns =
                expectedTable.getTableMetaData().getColumns();
        for (final Column column : allColumns)
        {
            final String columnName = column.getColumnName();
            if (!excludeColumnsList.contains(columnName))
            {
                keepColumnsList.add(column);
            }
        }

        return keepColumnsList.toArray(new Column[keepColumnsList.size()]);
    }

    /**
     * Make a <code>IDataSet</code> from the specified files.
     *
     * @param dataFiles
     *            Represents the array of dbUnit data files.
     * @return The composite dataset.
     * @throws DataSetException
     *             On dbUnit errors.
     */
    public IDataSet makeCompositeDataSet(final String[] dataFiles,
            final String dataFilesName) throws DataSetException
    {
        if (dataFileLoader == null)
        {
            throw new IllegalStateException(
                    "dataFileLoader is null; must configure or set it first");
        }

        final int count = dataFiles.length;
        log.debug("makeCompositeDataSet: {} dataFiles count={}", dataFilesName,
                count);
        if (count == 0)
        {
            log.info("makeCompositeDataSet: Specified zero {} data files",
                    dataFilesName);
        }

        final List list = new ArrayList();
        for (int i = 0; i < count; i++)
        {
            final IDataSet ds = dataFileLoader.load(dataFiles[i]);
            list.add(ds);
        }

        final IDataSet[] dataSet = (IDataSet[]) list.toArray(new IDataSet[] {});
        final IDataSet compositeDS = new CompositeDataSet(dataSet);
        return compositeDS;
    }

    /**
     * Apply the specified exclude and include column filters to the specified
     * table.
     *
     * @param table
     *            The table to apply the filters to.
     * @param excludeColumns
     *            The exclude filters; use null or empty array to mean exclude
     *            none.
     * @param includeColumns
     *            The include filters; use null to mean include all.
     * @return The filtered table.
     * @throws DataSetException
     */
    public ITable applyColumnFilters(final ITable table,
            final String[] excludeColumns, final String[] includeColumns)
            throws DataSetException
    {
        ITable filteredTable = table;

        if (table == null)
        {
            throw new IllegalArgumentException("table is null");
        }

        // note: dbunit interprets an empty inclusion filter array as one
        // not wanting to compare anything!
        if (includeColumns == null)
        {
            log.debug("applyColumnFilters: including columns=(all)");
        } else
        {
            log.debug("applyColumnFilters: including columns='{}'",
                    new Object[] {includeColumns});
            filteredTable = DefaultColumnFilter
                    .includedColumnsTable(filteredTable, includeColumns);
        }

        if (excludeColumns == null || excludeColumns.length == 0)
        {
            log.debug("applyColumnFilters: excluding columns=(none)");
        } else
        {
            log.debug("applyColumnFilters: excluding columns='{}'",
                    new Object[] {excludeColumns});
            filteredTable = DefaultColumnFilter
                    .excludedColumnsTable(filteredTable, excludeColumns);
        }

        return filteredTable;
    }

    /**
     * {@inheritDoc}
     */
    public IDataSet getPrepDataset()
    {
        return prepDataSet;
    }

    /**
     * {@inheritDoc}
     */
    public IDataSet getExpectedDataset()
    {
        return expectedDataSet;
    }

    /**
     * Get the databaseTester.
     *
     * @see {@link #databaseTester}.
     *
     * @return The databaseTester.
     */
    @Override
    public IDatabaseTester getDatabaseTester()
    {
        return databaseTester;
    }

    /**
     * Set the databaseTester.
     *
     * @see {@link #databaseTester}.
     *
     * @param databaseTester
     *            The databaseTester to set.
     */
    public void setDatabaseTester(final IDatabaseTester databaseTester)
    {
        this.databaseTester = databaseTester;
    }

    /**
     * Get the dataFileLoader.
     *
     * @see {@link #dataFileLoader}.
     *
     * @return The dataFileLoader.
     */
    public DataFileLoader getDataFileLoader()
    {
        return dataFileLoader;
    }

    /**
     * Set the dataFileLoader.
     *
     * @see {@link #dataFileLoader}.
     *
     * @param dataFileLoader
     *            The dataFileLoader to set.
     */
    public void setDataFileLoader(final DataFileLoader dataFileLoader)
    {
        this.dataFileLoader = dataFileLoader;
    }

    /**
     * Set the prepDs.
     *
     * @see {@link #prepDataSet}.
     *
     * @param prepDs
     *            The prepDs to set.
     */
    public void setPrepDs(final IDataSet prepDataSet)
    {
        this.prepDataSet = prepDataSet;
    }

    /**
     * Set the expectedDs.
     *
     * @see {@link #expectedDataSet}.
     *
     * @param expectedDs
     *            The expectedDs to set.
     */
    public void setExpectedDs(final IDataSet expectedDataSet)
    {
        this.expectedDataSet = expectedDataSet;
    }

    /**
     * Get the tableDefs.
     *
     * @see {@link #verifyTableDefs}.
     *
     * @return The tableDefs.
     */
    public VerifyTableDefinition[] getTableDefs()
    {
        return verifyTableDefs;
    }

    /**
     * Set the tableDefs.
     *
     * @see {@link #verifyTableDefs}.
     *
     * @param tableDefs
     *            The tableDefs to set.
     */
    public void setTableDefs(final VerifyTableDefinition[] verifyTableDefs)
    {
        this.verifyTableDefs = verifyTableDefs;
    }

    public ExpectedDataSetAndVerifyTableDefinitionVerifier getExpectedDataSetAndVerifyTableDefinitionVerifier()
    {
        return expectedDataSetAndVerifyTableDefinitionVerifier;
    }

    public void setExpectedDataSetAndVerifyTableDefinitionVerifier(
            final ExpectedDataSetAndVerifyTableDefinitionVerifier expectedDataSetAndVerifyTableDefinitionVerifier)
    {
        this.expectedDataSetAndVerifyTableDefinitionVerifier =
                expectedDataSetAndVerifyTableDefinitionVerifier;
    }
}
