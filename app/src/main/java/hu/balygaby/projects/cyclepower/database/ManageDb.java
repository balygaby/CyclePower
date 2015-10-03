package hu.balygaby.projects.cyclepower.database;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import hu.balygaby.projects.cyclepower.WorkoutService;
import hu.balygaby.projects.cyclepower.database.objects.WholeWorkout;
import hu.balygaby.projects.cyclepower.database.objects.WorkoutEntry;

/**
 * Do operations on the database.
 * Call getInstance to get the singleton instance.
 * Call setupDb to access methods.
 * After operations call closeDb.
 */
public class ManageDb {

    private static ManageDb instance = null; //SINGLETON CLASS

    Environment dbEnvironment;
    Database workoutDatabase;
    Cursor dbCursor;
    Context context;

    //<editor-fold desc="MAIN METHODS">
    public ManageDb(Context context) {
        dbEnvironment = null;
        workoutDatabase = null;
        this.context = context;
    }

    public static ManageDb getInstance(Context context) {
        if (instance == null) {
            instance = new ManageDb(context);
        }
        return instance;
    }

    /**
     * Sets up the the database
     *
     * @param readOnly Is the database read only.
     * @throws DatabaseException
     * @throws IllegalArgumentException
     */
    public void setupDb(boolean readOnly) throws DatabaseException, IllegalArgumentException {
        // Open the environment. Create it if it does not already exist.
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(!readOnly);
        envConfig.setReadOnly(readOnly);
        File root = context.getExternalFilesDir(null);
        File directory = new File(root + "/Berkeley");
        if (!directory.exists()) //noinspection ResultOfMethodCallIgnored
            directory.mkdir();
        dbEnvironment = new Environment(directory,
                envConfig);
        // Open the database. Create it if it does not already exist.
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(!readOnly);
        dbConfig.setReadOnly(readOnly);
        dbConfig.setDeferredWrite(false);
        dbConfig.setSortedDuplicates(false);//No duplicates
        workoutDatabase = dbEnvironment.openDatabase(null,
                "workoutDatabase",
                dbConfig);
    }

    /**
     * Close the database.
     */
    public int closeDb() {
        try {
            if (dbCursor != null) {
                dbCursor.close();
                dbCursor = null;
            }
            if (workoutDatabase != null) {
                workoutDatabase.close();
                workoutDatabase = null;
            }
            if (dbEnvironment != null) {
                dbEnvironment.close();
                dbEnvironment = null;
            }
            Log.d("ManageDb","Database closed");
            return WorkoutService.DATABASE_OK;
        } catch (DatabaseException dbe) {
            Toast.makeText(context, "Error closing database: " +
                    dbe.toString(), Toast.LENGTH_SHORT).show();
            return WorkoutService.DATABASE_CLOSE_ERROR;
        }
    }
    //</editor-fold>

    //<editor-fold desc="DATABASE WRITE">
    /**
     * Writes a record to the database.
     * <p>
     * If it returns {@link WorkoutService#DATABASE_NULL}, try again {@link #setupDb(boolean)}.
     * </p>
     *
     * @param key  The key in byte[].
     * @param data The data in byte[].
     * @return Result successful.
     * @throws DatabaseException
     */
    public int writeEntry(byte[] key, byte[] data){
        if (workoutDatabase != null) {
            DatabaseEntry aKey = new DatabaseEntry(key);
            DatabaseEntry aData = new DatabaseEntry(data);
            Log.d("writeDb", "writing: " + Arrays.toString(key) + "    data: " + Arrays.toString(data));

            try {
                if (workoutDatabase.put(null, aKey, aData) == OperationStatus.SUCCESS) {
                    return WorkoutService.DATABASE_OK;
                } else return WorkoutService.DATABASE_WRITE_PROBLEM;
            } catch (DatabaseException e) {
                Log.d("ManageDb","db write error: "+e);
                return WorkoutService.DATABASE_WRITE_PROBLEM;
            }
        } else {
            return WorkoutService.DATABASE_NULL;
        }
    }

    /**
     * Call this when starting and ending a new workout.
     * @param firstRecord The new first record.
     * @return Status.
     */
    private int writeFirstRecord(byte[] firstRecord) {
        if (workoutDatabase != null) {
            DatabaseEntry aKey = new DatabaseEntry(ByteConverter.getFirstRecordKey());
            DatabaseEntry aData = new DatabaseEntry(firstRecord);
            Log.d("writeDb", "writing first record: " + Arrays.toString(ByteConverter.getFirstRecordKey()) + "    data: " + Arrays.toString(firstRecord));

            try {
                OperationStatus operationStatus = workoutDatabase.put(null, aKey, aData);
                if ( operationStatus== OperationStatus.SUCCESS) {
                    Log.d("ManageDb","first record write success");
                    return WorkoutService.DATABASE_OK;
                }else if(operationStatus== OperationStatus.KEYEXIST){
                    Log.d("ManageDb","first record write success, record existed");
                    return WorkoutService.DATABASE_OK;
                } else {return WorkoutService.DATABASE_WRITE_PROBLEM;}
            } catch (DatabaseException e) {
                return WorkoutService.DATABASE_WRITE_PROBLEM;
            }
        } else {
            return WorkoutService.DATABASE_NULL;
        }
    }

    /**
     * Call this when starting a new workout.
     * @param startTime The new first record header (starting time of the  workout).
     * @return Status.
     */
    public int writeFirstRecordHeader(long startTime){
        byte[] existingFirstRecord = getFirstRecordBytes();
        byte[] newFirstRecord = ByteConverter.convertFirstRecordHeaderToByteArray(existingFirstRecord, startTime);
        return writeFirstRecord(newFirstRecord);
    }

    /**
     * Call this when ending a new workout (from the activity).
     * @param endTime End time of the session in milliseconds.
     * @param distance Whole distance of the workout.
     * @param work Whole work of the workout.
     * @return Status.
     */
    public int writeFirstRecordSummary(long endTime, double distance, double work){
        byte[] existingFirstRecord = getFirstRecordBytes();
        byte[] newFirstRecord = ByteConverter.convertFirstRecordSummaryToByteArray(existingFirstRecord, endTime, distance, work);
        Log.d("ManageDb","new first record: "+ Arrays.toString(newFirstRecord));
        return writeFirstRecord(newFirstRecord);
    }
    //</editor-fold>

    //<editor-fold desc="DATABASE READ">
    /**
     * Call this to be able to append to the existing first record.
     * @return The bytes of the existing first record.
     */
    private byte[] getFirstRecordBytes() throws DatabaseException{
        if ((dbEnvironment != null) && (workoutDatabase != null)) {
            Log.d("ManageDb", "getting last entry");
            try {
                dbCursor = workoutDatabase.openCursor(null, null);

                DatabaseEntry foundKey = new DatabaseEntry();
                DatabaseEntry foundData = new DatabaseEntry();

                if (dbCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    byte[] keyBytes = foundKey.getData();
                    byte[] dataBytes = foundData.getData();
                    if (!Arrays.equals(keyBytes,ByteConverter.getFirstRecordKey()))
                        return new byte[0];
                    return dataBytes;
                } else {
                    return new byte[0];
                }
            } catch (DatabaseException de) {
                Log.d("ManageDb", "Error getting first record bytes" + de);
            } finally {
                // Cursors must be closed.
                dbCursor.close();
            }
        }
        return new byte[0];
    }

    /**
     * Gets the references and summaries of workouts from the first record of the database.
     * @return The workouts as {@link WholeWorkout} objects.
     */
    public ArrayList<WholeWorkout> getWorkouts() throws Exception {
        if ((dbEnvironment != null) && (workoutDatabase != null)) {
            try {
                dbCursor = workoutDatabase.openCursor(null, null);

                DatabaseEntry foundKey = new DatabaseEntry();
                DatabaseEntry foundData = new DatabaseEntry();

                if (dbCursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    byte[] keyBytes = foundKey.getData();
                    byte[] dataBytes = foundData.getData();
                    if (!Arrays.equals(keyBytes,ByteConverter.getFirstRecordKey())) throw new Exception("No first entry");
                    return ByteConverter.getWorkoutsFromBytes(dataBytes);
                } else {
                    throw new Exception("No entries in database");
                }
            } catch (DatabaseException de) {
                Log.d("ManageDb", "Error accessing database." + de);
                throw de;
            } finally {
                // Cursors must be closed.
                dbCursor.close();
            }
        } else {
            throw new Exception("Environment or database is null!");
        }
    }

    /**
     * Gets the header and summary of the last workout from the databse first record.
     * @return The last {@link WholeWorkout} object.
     */
    public WholeWorkout getLastWorkout() throws Exception{
        ArrayList<WholeWorkout> wholeWorkouts = this.getWorkouts();
        return wholeWorkouts.get(wholeWorkouts.size()-1);
    }

    /**
     * Can read all the available data records of a specific workout from the database.
     * Pass the start time and end time of the workout as parameters, which can be
     * acquired from the {@link #getWorkouts()} method regarding the desired workout.
     * @param workoutStart Start time of the workout in milliseconds.
     * @param workoutEnd End time of the workout in milliseconds.
     * @return The specific workout.
     */
    public ArrayList<WorkoutEntry> readWorkouts(long workoutStart, long workoutEnd) throws Exception {
        ArrayList<WorkoutEntry> workoutEntries = new ArrayList<>();
        if ((dbEnvironment != null) && (workoutDatabase != null)) {
            try {
                dbCursor = workoutDatabase.openCursor(null, null);
                //moving the cursor to the start point
                DatabaseEntry theKey = new DatabaseEntry(ByteConverter.convertKeyToByteArray(workoutStart));
                DatabaseEntry theData = new DatabaseEntry();
                OperationStatus searchStatus = dbCursor.getSearchKeyRange(theKey, theData,LockMode.DEFAULT);
                if (searchStatus != OperationStatus.SUCCESS) throw new Exception("Couldn't find search key");

                DatabaseEntry foundKey = new DatabaseEntry();
                DatabaseEntry foundData = new DatabaseEntry();


                // To iterate, just call getNext() from the start record until the end database record has
                // been read. All cursor operations return an OperationStatus, so just
                // read until we no longer see OperationStatus.SUCCESS
                while (dbCursor.getNext(foundKey, foundData, LockMode.DEFAULT) ==
                        OperationStatus.SUCCESS) {
                    // getData() on the DatabaseEntry objects returns the byte array
                    byte[] keyBytes = foundKey.getData();
                    byte[] dataBytes = foundData.getData();

                    if (Arrays.equals(keyBytes,ByteConverter.convertKeyToByteArray(workoutEnd))) break; //when we reach the start of the next workout, it's the end
                    //todo when filling out the header in the main activity at the end of the session, insert a record to the end, with the same key as the specified endTime of the workout
                    //adding record to the entry array right away
                    workoutEntries.add(new WorkoutEntry(ByteConverter.getTimeFromBytes(keyBytes),
                            ByteConverter.getDistanceFromBytes(dataBytes),
                            ByteConverter.getWorkFromBytes(dataBytes),
                            ByteConverter.getSpeedFromBytes(dataBytes),
                            ByteConverter.getCadenceFromBytes(dataBytes),
                            ByteConverter.getPowerFromBytes(dataBytes),
                            ByteConverter.getTorqueFromBytes(dataBytes),
                            ByteConverter.getLatitudeFromBytes(dataBytes),
                            ByteConverter.getLongitudeFromBytes(dataBytes)));
                }
            } catch (DatabaseException de) {
                Log.d("ManageDb","Error accessing database." + de);
                throw de;
            } finally {
                // Cursors must be closed.
                dbCursor.close();
            }//todo exceptions etc
        }
        return workoutEntries;
    }

    /**
     * Call this to eventually restore the main service fields after a breakdown.
     *
     * @return The latest values from the last workout.
     */
    public WorkoutEntry getLastEntry() throws Exception {
        if ((dbEnvironment != null) && (workoutDatabase != null)) {
            Log.d("ManageDb", "getting last entry");
            try {
                dbCursor = workoutDatabase.openCursor(null, null);

                DatabaseEntry foundKey = new DatabaseEntry();
                DatabaseEntry foundData = new DatabaseEntry();

                if (dbCursor.getPrev(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    byte[] keyBytes = foundKey.getData();
                    byte[] dataBytes = foundData.getData();
                    return new WorkoutEntry(ByteConverter.getTimeFromBytes(keyBytes),
                            ByteConverter.getDistanceFromBytes(dataBytes),
                            ByteConverter.getWorkFromBytes(dataBytes),
                            ByteConverter.getSpeedFromBytes(dataBytes),
                            ByteConverter.getCadenceFromBytes(dataBytes),
                            ByteConverter.getPowerFromBytes(dataBytes),
                            ByteConverter.getTorqueFromBytes(dataBytes),
                            ByteConverter.getLatitudeFromBytes(dataBytes),
                            ByteConverter.getLongitudeFromBytes(dataBytes));
                } else {
                    throw new Exception("No records in database");
                }
            } catch (DatabaseException de) {
                Log.d("ManageDb", "Error accessing database." + de);
                throw de;
            } finally {
                // Cursors must be closed.
                dbCursor.close();
            }
        }else {
            throw new Exception("Environment or database is null");
        }
    }
    //</editor-fold>

    //<editor-fold desc="STATUS GETTER METHODS">

    /**
     * Is a workout in progress. Call this on the service onCreate.
     * @return Is a workout in progress.
     */
    public boolean isWorkoutInProgress() {
        ArrayList<WholeWorkout> wholeWorkouts;
        try {
            wholeWorkouts = getWorkouts();
        } catch (Exception e) {
            return false;
        }
        return wholeWorkouts.size() != 0 && wholeWorkouts.get(wholeWorkouts.size() - 1).getEndTime() == 0;//not ended yet, nothing in endTime!
    }

    public boolean isDatabaseSet(){//todo test this method, then use it
        try {
            if (workoutDatabase.getEnvironment() != null) return true;
        } catch (Exception e) {
            Log.d("ManageDb", "No database available when requesting isDatabaseSet");
        }
        return false;
    }
    //</editor-fold>
}