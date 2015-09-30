package hu.balygaby.projects.cyclepower.database;

import java.util.ArrayList;

import hu.balygaby.projects.cyclepower.database.objects.WholeWorkout;

public class ByteConverter {

    private static final int TIME_LENGTH = 6;
    private static final int DISTANCE_LENGTH = 3;
    private static final int WORK_LENGTH = 3;
    private static final int SPEED_LENGTH = 2;
    private static final int CADENCE_LENGTH = 2;
    private static final int POWER_LENGTH = 2;
    private static final int TORQUE_LENGTH = 2;
    private static final int LATITUDE_LENGTH = 4;
    private static final int LONGITUDE_LENGTH = 4;
    private static final int COMBINED_LENGTH = DISTANCE_LENGTH + WORK_LENGTH + SPEED_LENGTH + CADENCE_LENGTH + POWER_LENGTH + TORQUE_LENGTH + LATITUDE_LENGTH + LONGITUDE_LENGTH;

    private static final int SUMMARY_LENGTH = 12;
    private static final int WORKOUT_LENGTH = TIME_LENGTH + SUMMARY_LENGTH;

    private static final int DISTANCE_DECIMALS = 0;
    private static final int WORK_DECIMALS = 0;
    private static final int SPEED_DECIMALS = 1;
    private static final int CADENCE_DECIMALS = 1;
    private static final int POWER_DECIMALS = 1;
    private static final int TORQUE_DECIMALS = 2;
    private static final int LATITUDE_DECIMALS = 6;
    private static final int LONGITUDE_DECIMALS = 6;


    private static byte[] longToBytes(int byteNumber, long input){
        byte[] output = new byte[byteNumber];
        for (int i = 0; i<byteNumber; i++){
            output[i] = (byte)(input / ((long)Math.pow(256, byteNumber-1-i)) - 128);//-128 offset in java byte
            input = input % ((long)Math.pow(256, byteNumber-1-i));
        }
        return output;
    }

    private static long bytesToLong(byte[] input){
        long output = 0;
        int byteNumber = input.length;
        for (int i=0; i<byteNumber;i++){
            output+=(((long)(input[i]))+128)*((long)Math.pow(256, byteNumber-1-i));
        }
        return output;
    }

    private static long doubleToLong(int decimals, double input){
        return ((long)(input * Math.pow(10,decimals)));
    }

    private static double longToDouble(int decimals, long input){
        return ((double)input) / Math.pow(10,decimals);
    }

    public static byte[] convertKeyToByteArray(long currentTimeInMillis){
        return longToBytes(TIME_LENGTH, currentTimeInMillis);
    }

    public static byte[] convertDataToByteArray(double distance, double work, double speed, double cadence,
                                                double power, double torque, double latitude, double longitude){

        int dstPos = 0;
        byte[] data = new byte[COMBINED_LENGTH];
        System.arraycopy(longToBytes(DISTANCE_LENGTH,doubleToLong(DISTANCE_DECIMALS,distance)),0,data,dstPos, DISTANCE_LENGTH);
        dstPos += DISTANCE_LENGTH;
        System.arraycopy(longToBytes(WORK_LENGTH,doubleToLong(WORK_DECIMALS,work)),0,data,dstPos, WORK_LENGTH);
        dstPos += WORK_LENGTH;
        System.arraycopy(longToBytes(SPEED_LENGTH,doubleToLong(SPEED_DECIMALS,speed)),0,data,dstPos, SPEED_LENGTH);
        dstPos += SPEED_LENGTH;
        System.arraycopy(longToBytes(CADENCE_LENGTH,doubleToLong(CADENCE_DECIMALS,cadence)),0,data,dstPos, CADENCE_LENGTH);
        dstPos += CADENCE_LENGTH;
        System.arraycopy(longToBytes(POWER_LENGTH,doubleToLong(POWER_DECIMALS,power)),0,data,dstPos, POWER_LENGTH);
        dstPos += POWER_LENGTH;
        System.arraycopy(longToBytes(TORQUE_LENGTH,doubleToLong(TORQUE_DECIMALS,torque)),0,data,dstPos, TORQUE_LENGTH);
        dstPos += TORQUE_LENGTH;
        System.arraycopy(longToBytes(LATITUDE_LENGTH,doubleToLong(LATITUDE_DECIMALS,latitude)),0,data,dstPos, LATITUDE_LENGTH);
        dstPos += LATITUDE_LENGTH;
        System.arraycopy(longToBytes(LONGITUDE_LENGTH,doubleToLong(LONGITUDE_DECIMALS,longitude)),0,data,dstPos, LONGITUDE_LENGTH);

        return data;
    }

    /**
     * The first record key to the database is always the 0 epoch time.
     * @return Six -128 bytes.
     */
    public static byte[] getFirstRecordKey(){
        byte[] firstRecordKey = new byte[TIME_LENGTH];
        for (int i=0; i<TIME_LENGTH; i++){
            firstRecordKey[i] = Byte.MIN_VALUE;
        }
        return firstRecordKey;
    }

    /**
     * The dummy data for workout closing entries, written from the activity.
     * @return +127 bytes to fill the data field.
     */
    public static byte[] getClosingRecordData(){
        byte[] closingRecordData = new byte[COMBINED_LENGTH];
        for (int i=0; i<COMBINED_LENGTH; i++){
            closingRecordData[i] = Byte.MAX_VALUE;
        }
        return closingRecordData;
    }

    /**
     * Adds a new header to the database headers records.
     * <p>
     *     IMPORTANT: the old header needs to be extracted as byte[] beforehand and passed as a parameter.
     * </p>
     * @param existingFirstRecord The old first record as byte[].
     * @param newHeader The time of the new header in milliseconds.
     * @return The new first record in byte[].
     */
    public static byte[] convertFirstRecordHeaderToByteArray(byte[] existingFirstRecord, long newHeader){
        byte[] headerBytes = longToBytes(TIME_LENGTH, newHeader); //todo test this method ald also isWorkoutInProgress method
        byte[] timeBytes = longToBytes(TIME_LENGTH, 0);//dummy
        byte[] distanceBytes = longToBytes(DISTANCE_LENGTH, doubleToLong(DISTANCE_DECIMALS, 0.0));//dummy
        byte[] workBytes = longToBytes(WORK_LENGTH,doubleToLong(WORK_DECIMALS,0.0));//dummy
        byte[] newFirstRecord = new byte[existingFirstRecord.length + headerBytes.length + SUMMARY_LENGTH];//there's the blank space for summary
        System.arraycopy(existingFirstRecord,0,newFirstRecord,0,existingFirstRecord.length);
        System.arraycopy(headerBytes,0,newFirstRecord,existingFirstRecord.length,headerBytes.length);
        System.arraycopy(timeBytes,0,newFirstRecord,existingFirstRecord.length+TIME_LENGTH,TIME_LENGTH);
        System.arraycopy(distanceBytes,0,newFirstRecord,existingFirstRecord.length+TIME_LENGTH+TIME_LENGTH, DISTANCE_LENGTH);
        System.arraycopy(workBytes,0,newFirstRecord,existingFirstRecord.length+TIME_LENGTH+TIME_LENGTH+DISTANCE_LENGTH,WORK_LENGTH);
        return newFirstRecord;
    }

    public static byte[] convertFirstRecordSummaryToByteArray(byte[] existingFirstRecord, long time, double distance, double work){
        byte[] timeBytes = longToBytes(TIME_LENGTH, time);
        byte[] distanceBytes = longToBytes(DISTANCE_LENGTH, doubleToLong(DISTANCE_DECIMALS, distance));
        byte[] workBytes = longToBytes(WORK_LENGTH,doubleToLong(WORK_DECIMALS,work));
        System.arraycopy(timeBytes,0,existingFirstRecord,existingFirstRecord.length-TIME_LENGTH-DISTANCE_LENGTH-WORK_LENGTH,timeBytes.length);
        System.arraycopy(distanceBytes,0,existingFirstRecord,existingFirstRecord.length-DISTANCE_LENGTH-WORK_LENGTH,distanceBytes.length);
        System.arraycopy(workBytes,0,existingFirstRecord,existingFirstRecord.length-WORK_LENGTH,workBytes.length);
        return existingFirstRecord;
    }

    public static ArrayList<WholeWorkout> getWorkoutsFromBytes(byte[] firstRecordData){
        int workoutCount = firstRecordData.length / WORKOUT_LENGTH;
        ArrayList<WholeWorkout> wholeWorkouts = new ArrayList<>();
        for (int i=0; i<workoutCount; i++){
            byte[] startTimeBytes = new byte[TIME_LENGTH];
            System.arraycopy(firstRecordData,i*WORKOUT_LENGTH,startTimeBytes,0,TIME_LENGTH);
            long startTime = bytesToLong(startTimeBytes);
            byte[] endTimeBytes = new byte[TIME_LENGTH];
            System.arraycopy(firstRecordData,i*WORKOUT_LENGTH+TIME_LENGTH,endTimeBytes,0,TIME_LENGTH);
            long endTime = bytesToLong(endTimeBytes);
            byte[] distanceBytes = new byte[DISTANCE_LENGTH];
            System.arraycopy(firstRecordData,i*WORKOUT_LENGTH+TIME_LENGTH+TIME_LENGTH,distanceBytes,0,DISTANCE_LENGTH);
            double distance = longToDouble(DISTANCE_DECIMALS, bytesToLong(distanceBytes));
            byte[] workBytes = new byte[WORK_LENGTH];
            System.arraycopy(firstRecordData,i*WORKOUT_LENGTH+TIME_LENGTH+TIME_LENGTH+DISTANCE_LENGTH,workBytes,0,WORK_LENGTH);
            double work = longToDouble(WORK_DECIMALS,bytesToLong(workBytes));
            wholeWorkouts.add(new WholeWorkout(startTime, endTime, distance, work));
        }
        return wholeWorkouts;
    }

    public static long getTimeFromBytes(byte[] keyBytes){
        return bytesToLong(keyBytes);
    }

    public static double getDistanceFromBytes(byte[] dataBytes){
        byte[] distanceBytes = new byte[DISTANCE_LENGTH];
        final int start = 0;
        System.arraycopy(dataBytes,start,distanceBytes,0,DISTANCE_LENGTH);
        return longToDouble(DISTANCE_DECIMALS, bytesToLong(distanceBytes));
    }

    public static double getWorkFromBytes(byte[] dataBytes){
        byte[] workBytes = new byte[WORK_LENGTH];
        final int start = DISTANCE_LENGTH;
        System.arraycopy(dataBytes,start,workBytes,0,DISTANCE_LENGTH);
        return longToDouble(WORK_DECIMALS,bytesToLong(workBytes));
    }

    public static double getSpeedFromBytes(byte[] dataBytes){
        byte[] speedBytes = new byte[SPEED_LENGTH];
        final int start = DISTANCE_LENGTH + WORK_LENGTH;
        System.arraycopy(dataBytes,start,speedBytes,0,SPEED_LENGTH);
        return longToDouble(SPEED_DECIMALS,bytesToLong(speedBytes));
    }

    public static double getCadenceFromBytes(byte[] dataBytes){
        byte[] cadenceBytes = new byte[CADENCE_LENGTH];
        final int start = DISTANCE_LENGTH + WORK_LENGTH + SPEED_LENGTH;
        System.arraycopy(dataBytes,start,cadenceBytes,0,CADENCE_LENGTH);
        return longToDouble(CADENCE_DECIMALS,bytesToLong(cadenceBytes));
    }

    public static double getPowerFromBytes(byte[] dataBytes){
        byte[] powerBytes = new byte[POWER_LENGTH];
        final int start = DISTANCE_LENGTH + WORK_LENGTH + SPEED_LENGTH + CADENCE_LENGTH;
        System.arraycopy(dataBytes,start,powerBytes,0,POWER_LENGTH);
        return longToDouble(POWER_DECIMALS,bytesToLong(powerBytes));
    }

    public static double getTorqueFromBytes(byte[] dataBytes){
        byte[] torqueBytes = new byte[TORQUE_LENGTH];
        final int start = DISTANCE_LENGTH + WORK_LENGTH + SPEED_LENGTH + CADENCE_LENGTH + POWER_LENGTH;
        System.arraycopy(dataBytes,start,torqueBytes,0,TORQUE_LENGTH);
        return longToDouble(TORQUE_DECIMALS,bytesToLong(torqueBytes));
    }

    public static double getLatitudeFromBytes(byte[] dataBytes){
        byte[] latitudeBytes = new byte[LATITUDE_LENGTH];
        final int start = DISTANCE_LENGTH + WORK_LENGTH + SPEED_LENGTH + CADENCE_LENGTH + POWER_LENGTH + TORQUE_LENGTH;
        System.arraycopy(dataBytes,start,latitudeBytes,0,LATITUDE_LENGTH);
        return longToDouble(LATITUDE_DECIMALS,bytesToLong(latitudeBytes));
    }

    public static double getLongitudeFromBytes(byte[] dataBytes){
        byte[] longitudeBytes = new byte[LONGITUDE_LENGTH];
        final int start = DISTANCE_LENGTH + WORK_LENGTH + SPEED_LENGTH + CADENCE_LENGTH + POWER_LENGTH + TORQUE_LENGTH + LATITUDE_LENGTH;
        System.arraycopy(dataBytes,start,longitudeBytes,0,LONGITUDE_LENGTH);
        return longToDouble(LONGITUDE_DECIMALS,bytesToLong(longitudeBytes));
    }
}

