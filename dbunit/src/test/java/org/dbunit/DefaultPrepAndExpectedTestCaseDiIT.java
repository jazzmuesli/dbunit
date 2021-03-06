package org.dbunit;

import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.util.fileloader.DataFileLoader;
import org.dbunit.util.fileloader.FlatXmlDataFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test of composition of the PrepAndExpected (simulated DI).
 * 
 * @author Jeff Jensen jeffjensen AT users.sourceforge.net
 * @author Last changed by: $Author$
 * @version $Revision$ $Date$
 * @since 2.4.8
 */
public class DefaultPrepAndExpectedTestCaseDiIT extends TestCase {
    private static final String PREP_DATA_FILE_NAME = "/xml/flatXmlDataSetTest.xml";
    private static final String EXP_DATA_FILE_NAME = "/xml/flatXmlDataSetTestChanged.xml";

    private static final VerifyTableDefinition TEST_TABLE =
            makeVerifyTableDefinition("TEST_TABLE");
    private static final VerifyTableDefinition SECOND_TABLE =
            makeVerifyTableDefinition("SECOND_TABLE");
    private static final VerifyTableDefinition EMPTY_TABLE =
            makeVerifyTableDefinition("EMPTY_TABLE");
    private static final VerifyTableDefinition PK_TABLE =
            makeVerifyTableDefinition("PK_TABLE");
    private static final VerifyTableDefinition ONLY_PK_TABLE =
            makeVerifyTableDefinition("ONLY_PK_TABLE");
    private static final VerifyTableDefinition EMPTY_MULTITYPE_TABLE =
            makeVerifyTableDefinition("EMPTY_MULTITYPE_TABLE");

    private final DataFileLoader dataFileLoader = new FlatXmlDataFileLoader();

    private DatabaseEnvironment dbEnv;
    private IDatabaseConnection connection;
    private IDatabaseTester databaseTester;

    private DefaultPrepAndExpectedTestCase tc;

    private static VerifyTableDefinition makeVerifyTableDefinition(
            String tableName)
    {
        return new VerifyTableDefinition(tableName, new String[] {});
    }

    protected void setUp() throws Exception {
        dbEnv = DatabaseEnvironment.getInstance();
        connection = dbEnv.getConnection();
        databaseTester = new DefaultDatabaseTester(connection);

        tc = new DefaultPrepAndExpectedTestCase(dataFileLoader, databaseTester);

        super.setUp();
    }

    public void testSuccessRun() throws Exception {
        // use same files to have no data comparison fails
        String[] prepDataFiles = {PREP_DATA_FILE_NAME};
        String[] expectedDataFiles = {PREP_DATA_FILE_NAME};
        VerifyTableDefinition[] tables = {TEST_TABLE, SECOND_TABLE, EMPTY_TABLE,
                PK_TABLE, ONLY_PK_TABLE, EMPTY_MULTITYPE_TABLE};

        tc.configureTest(tables, prepDataFiles, expectedDataFiles);
        tc.preTest();

        // skip modifying data and just verify the insert

        // reopen connection as DefaultOperationListener closes it after inserts
        // maybe we need a KeepConnectionOpenOperationListener class?!
        connection = dbEnv.getConnection();
        databaseTester = new DefaultDatabaseTester(connection);
        tc.setDatabaseTester(databaseTester);

        tc.postTest();
    }

    public void testFailRun() throws Exception {
        String[] prepDataFiles = {PREP_DATA_FILE_NAME};
        String[] expectedDataFiles = {EXP_DATA_FILE_NAME};
        VerifyTableDefinition[] tables = {TEST_TABLE, SECOND_TABLE, EMPTY_TABLE,
                PK_TABLE, ONLY_PK_TABLE, EMPTY_MULTITYPE_TABLE};

        tc.configureTest(tables, prepDataFiles, expectedDataFiles);
        tc.preTest();

        // skip modifying data and just verify the insert

        // reopen connection as DefaultOperationListener closes it after inserts
        // maybe we need a KeepConnectionOpenOperationListener class?!
        connection = dbEnv.getConnection();
        databaseTester = new DefaultDatabaseTester(connection);
        tc.setDatabaseTester(databaseTester);

        try {
            tc.postTest();
            fail("Did not catch expected exception:"
                    + " junit.framework.ComparisonFailure");
        } catch (ComparisonFailure e) {
            // test passes
        }
    }
}
