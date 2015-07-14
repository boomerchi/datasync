package com.socrata.datasync.model;

import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.LocationColumn;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesFile;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Series of tests to validate that the control file model produces a correct control file.
 *
 * Created by franklinwilliams
 */
public class ControlFileModelTest extends TestBase {

    ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @Test
    public void testColumnUpdates() throws IOException, LongRunningQueryException, InterruptedException, SodaError {
        ControlFile cf = getTestControlFile();
        ControlFileModel model = getTestModel(cf);

        String fieldName = "test";
        model.updateColumnAtPosition(fieldName,0);
        TestCase.assertEquals(model.getColumnAtPosition(0), fieldName);
    }

    //Can I get out the same control file that I put in?
    @Test
    public void testSerialization() throws IOException, LongRunningQueryException, InterruptedException, SodaError {

        ControlFile cf = getTestControlFile();
        ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        String originalValue = mapper.writeValueAsString(cf);
        ControlFileModel model = getTestModel(cf);
        String newValue = model.getControlFileContents();
        TestCase.assertEquals(originalValue,newValue);
    }

    @Test
    public void testIgnoredColumns() throws IOException, LongRunningQueryException, InterruptedException, SodaError {
        ControlFile cf = getTestControlFile();
        ControlFileModel model = getTestModel(cf);
        int columnLengthBeforeUpdate = model.getColumnCount();
        //Verify that the location column is part of the ignored list
        TestCase.assertTrue(model.isIgnored("foo"));
        //Now add it back to the columns list
        model.updateColumnAtPosition("foo",0);
        //Verify that it is no longer part of the ignored columns list.
        TestCase.assertFalse(model.isIgnored("foo"));
        // Ignoring a column should not change how many columns there are.
        // Verify that we have the same number of columns in the control file as we did when we started
        TestCase.assertEquals(model.getColumnCount(), columnLengthBeforeUpdate);
    }

    @Test
    public void testHasHeaderRow() throws IOException, LongRunningQueryException, InterruptedException, SodaError {
        //Assert that we are skipping the first row when has header row == 1
        ControlFile cf = getTestControlFile();
        ControlFileModel model = getTestModel(cf);
        model.setHasHeaderRow(true);
        TestCase.assertEquals((int) model.getControlFile().getFileTypeControl().skip,1);
        //Assert that we decrement by 1 when the header row is turned off
        model.setRowsToSkip(5);
        model.setHasHeaderRow(false);
        TestCase.assertEquals((int) model.getControlFile().getFileTypeControl().skip,4);
    }

    @Test
    public void testSyntheticColumns() throws IOException, LongRunningQueryException, InterruptedException, SodaError {
        ControlFile cf = getTestControlFile();
        ControlFileModel model = getTestModel(cf);

        String fieldName = "testLoc";
        LocationColumn location = new LocationColumn();
        location.address = "1234 fake street";
        location.latitude = "90";
        location.longitude = "90";
        //Test that we can add to the synthetic location when none exist
        model.setSyntheticLocation(fieldName,location);
        TestCase.assertEquals(model.getSyntheticLocations().get(fieldName),location);
        TestCase.assertFalse(model.getControlFile().getFileTypeControl().useSocrataGeocoding);

        //Test that we can add another one when one already exists
        String anotherColumn = "anotherColumn";
        LocationColumn newLocation = new LocationColumn();
        newLocation.address = "742 evergreen terrace";
        model.setSyntheticLocation(anotherColumn,newLocation);
        TestCase.assertEquals(model.getSyntheticLocations().get(anotherColumn), newLocation);
    }

    private ControlFile getTestControlFile() throws IOException {
        File controlFile = new File("src/test/resources/datasync_unit_test_three_rows_control.json");
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);
        cf.getFileTypeControl().filePath = "src/test/resources/datasync_unit_test_three_rows.csv";
        return cf;
    }

    private ControlFileModel getTestModel(ControlFile cf) throws IOException, LongRunningQueryException, InterruptedException, SodaError {
        File configFile = new File(PATH_TO_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        System.out.println("Attempting to sign-in: " + userPrefs.getUsername() + " with password: " + userPrefs.getPassword());
        DatasetModel datasetModel = new DatasetModel(userPrefs.getDomain(),userPrefs.getUsername(),userPrefs.getPassword(),userPrefs.getAPIKey(),TestBase.UNITTEST_DATASET_ID);
        ControlFileModel model = new ControlFileModel(cf,datasetModel);
        return model;
    }

}
