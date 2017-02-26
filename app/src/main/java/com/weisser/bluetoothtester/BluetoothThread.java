package com.weisser.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.*;
import java.util.Set;
import java.util.UUID;

/**
 * Generic BluetoothThread.
 *
 *
 * Android and Bluetooth Bee:
 *   http://www.elecfreaks.com/677.html
 *
 *
 *   http://wiki.seeedstudio.com/wiki/Bluetooth_Bee
 *
 * Created by stefan on 11/02/2017.
 */
public class BluetoothThread extends Thread {
    private static final String LOGTAG = "BluetoothThread";

    protected BluetoothSocket socket;
    protected InputStream inputStream;
    protected InputStreamReader inputStreamReader;
    protected BufferedReader reader;

    // Thread state
    protected volatile boolean isRunning = true;

    private static int instanceCount = 0;

    protected String macAddress;

    private double currentTemperature = 0.0;

    /**
     * Creates a new bluetoothThread.
     */
    public BluetoothThread() {
        instanceCount++;
    }

    /**
     * Initiates a bluetooth connection for the given macaddress.
     * Result will be an open socket, stored in the socket member variable.
     * If the socket could be opened successfully, the method will also call openConnection().
     *
     * Who calls this?
     *
     * @param macAddress
     * @return The errorcode.
     */

    public int initConnection(String macAddress) {
        int rc;

        this.socket = null;
        this.macAddress = macAddress;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.e(LOGTAG, "Device does not support bluetooth.");

            return GPSProvider.ERR_NO_ADAPTER;
        } else {
            Log.d(LOGTAG, "BluetoothAdapter: " + bluetoothAdapter);

            // show paired devices (just for info)
            showPairedDevices(bluetoothAdapter);

            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);

            if (device == null) {
                Log.e(LOGTAG, "Could not open specified device." + getThreadId());

                return GPSProvider.ERR_NO_DEVICE;
            } else {
                Log.d(LOGTAG, "Got the specified device:" + device);
                Log.i(LOGTAG, "Device class: " + device.getBluetoothClass());

                rc = createSocket(device);
                if (rc != GPSProvider.SUCCESS) {
                    return rc;
                }

                // Cancel discovery because it will slow down the connection
                bluetoothAdapter.cancelDiscovery();

                Log.d(LOGTAG, "Cancel discovery done.");

                int maxConnectRetries = 1;
                do {
                    rc = connectSocket();
                } while (--maxConnectRetries > 0 && rc != GPSProvider.SUCCESS);

                if (rc != GPSProvider.SUCCESS) {
                    return rc;
                }

                rc = openInputStream();
                if (rc != GPSProvider.SUCCESS) {
                    return rc;
                }

                inputStreamReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(inputStreamReader);

                Log.d(LOGTAG, "After connect().");

                return GPSProvider.SUCCESS;
            }
        }
    }


    private void showPairedDevices(BluetoothAdapter bluetoothAdapter) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                Log.d(LOGTAG, "Bonded device: " + deviceHardwareAddress + ", Name: " + deviceName);
            }
        }
    }

    private int createSocket(BluetoothDevice device) {
        // Get the socket
        try {
            //UUID uuid = getUUID(device);
            UUID uuid = getUUIDDefault(device);

            if (uuid != null) {
                Log.d(LOGTAG, "Using UUID: " + uuid.toString());

                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);

                Log.d(LOGTAG, "Got a socket for " +getThreadId() + " " + socket);
            } else {
                return GPSProvider.ERR_NO_UUIDS;
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "IOException " + getThreadId(), e);

            return GPSProvider.ERR_NO_SOCKET;
        }

        return GPSProvider.SUCCESS;
    }

    private void sendCharacter(byte b) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(b);
            os.flush();
        } catch (IOException e) {
            Log.e(LOGTAG, "sendCharacter", e);
        }
    }


    private UUID getUUIDDefault(BluetoothDevice device) {
        // Default for bluetooth Bee: "00001101-0000-1000-8000-00805F9B34FB", also recommended by Android Docs for serial boards

        final String defaultUUIDString = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(defaultUUIDString);

        return uuid;
    }


    private UUID getUUID(BluetoothDevice device) {
        final int USE_UUID_INDEX = 0;

        // Gets cached UUIDs
        ParcelUuid[] uuids;
        uuids = device.getUuids();
        showUUIDs(uuids);

        if (uuids != null && uuids.length >= USE_UUID_INDEX + 1) {

            UUID uuid = uuids[USE_UUID_INDEX].getUuid();

            return uuid;
        } else {
            Log.e(LOGTAG, "No (cached) UUIDs");

            // we can try to fetch them fresh from the device now
            boolean result = device.fetchUuidsWithSdp();

            if (result) {
                Log.d(LOGTAG, "fetchUuidsWithSdp() succeeded.");
            } else {
                Log.d(LOGTAG, "fetchUuidsWithSdp() failed.");
            }

            uuids = device.getUuids();
            showUUIDs(uuids);

            if (uuids != null && uuids.length >= USE_UUID_INDEX + 1) {
                UUID uuid = uuids[USE_UUID_INDEX].getUuid();
                return uuid;
            }
        }

        return null;
    }


    private void showUUIDs(ParcelUuid[] uuids) {
        if (uuids != null) {
            for (int i = 0; i < uuids.length; i++) {
                Log.d(LOGTAG, "UUID[" + i + "] = " + uuids[i].toString());
            }
        }
    }

    private int connectSocket() {
        // At this point we do have a socket...
        try {
            socket.connect();

            return GPSProvider.SUCCESS;
        } catch (IOException e) {
            Log.e(LOGTAG, "IOException during socket.connect() " + getThreadId(), e);

            return GPSProvider.ERR_SOCKET_CONNECT;
        }
    }

    private int openInputStream() {
        try {
            // Get the input and output streams, using temp objects because
            // member streams are final
            InputStream tmpIn = socket.getInputStream();

            inputStream = tmpIn;

            return GPSProvider.SUCCESS;
        } catch (IOException e) {
            Log.e(LOGTAG, "IOException socket.getInputStream() " + getThreadId(), e);

            return GPSProvider.ERR_SOCKET_INPUT_STREAM;
        }
    }

    /**
     * Threads main loop.
     *
     * Will end, if the socket is no longer connected.
     * Will end, if isRunning gets set to false.
     */
    @Override
    public void run() {
        this.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        // Keep listening to the InputStream until an exception occurs
        while (!Thread.currentThread().isInterrupted() && isRunning && socket.isConnected()) {
            try {
                // Read all available bytes
                //updateParserConnectionState();

                //logThreadCount();

                // TODO Convert char to byte using ASCII
                sendCharacter((byte)'h');

                sendCharacter((byte)'l');

                sendCharacter((byte)'t');

                parseTemperature();

                //parse();
                /*
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    closeConnection();
                    onFinishThread();
                }
                */

            } catch (IOException ee) {
                Log.e(LOGTAG, "IOException", ee);
                if (ee.getMessage().contains("socket closed")) {

                    // TODO Check order of these calls!
                    updateParserConnectionState();
                    closeConnection();

                    isRunning = false;
                }

                if (ee.getMessage().contains("Software caused connection abort")) {
                    // TODO Check order of these calls!
                    updateParserConnectionState();
                    closeConnection();

                    isRunning = false;
                }
            }

            //Log.d(LOGTAG, "instanceCount(" + this + ") = " + instanceCount);

        }

        closeConnection();

        // Additional callback
        onFinishThread();

        Log.d(LOGTAG, "Finishing this thread: " + this);
    }

    public double readTemperature() {
        return currentTemperature;
    }


    protected void parse() throws IOException {
        final int readAtOnce = 5;

        for (int i = 0; i < readAtOnce; i++) {
            if (reader.ready()) {
                String decoded = reader.readLine();
                Log.d(LOGTAG, macAddress + " got line: " + decoded);
            }
        }
    }

    protected void parseTemperature() throws IOException {
        if (reader.ready()) {
            String decoded = reader.readLine();
            Log.d(LOGTAG, macAddress + " got line: " + decoded);

            try {
                double tmpTemperature = Double.parseDouble(decoded);
                currentTemperature = tmpTemperature;
            } catch (NumberFormatException e) {

            }
        }
    }

    protected void onFinishThread() {
        Log.d(LOGTAG, "onFinishThread");
    }

    private void logThreadCount() {
        // Log.d(LOGTAG, "ThreadCount: " + activeCount());
    }

    /**
     * Updates the connectionState in the NMEAParserState object.
     * Method only updates the info there.
     */
    protected void updateParserConnectionState() {
        final String updateConnectionState = "updateParserConnectionState()";
        boolean socketConnected = false;
        boolean readerReady = false;
        int streamAvailableBytes = 0;

        try {
            socketConnected = socket.isConnected();
        } catch (Exception e) {
            Log.e(LOGTAG, updateConnectionState, e);
        }

        try {
            if (reader != null) {
                readerReady = reader.ready();
            }
        } catch (Exception e) {
            Log.e(LOGTAG, updateConnectionState, e);
        }

        try {
            streamAvailableBytes = inputStream.available();
        } catch (Exception e) {
            Log.e(LOGTAG, updateConnectionState, e);
        }

        // TODO Do we really need it? Does it work at all?
        onUpdateConnectionState(socketConnected, readerReady, streamAvailableBytes);
    }

    protected void onUpdateConnectionState(boolean socketConnected, boolean readerReady, int streamAvailableBytes) {

    }


    /**
     * Closes the connection to the socket.
     *
     * Also closes the inputStreamReader and the reader.
     */
    protected void closeConnection() {
        Log.d(LOGTAG, "closeConnection");

        if (reader != null) {
            try {
                reader.close();
                reader = null;
                Log.d(LOGTAG, "reader close() ok");
            } catch (IOException e) {
                Log.e(LOGTAG, "IOException during reader.close()", e);
            }
        }

        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
                inputStreamReader = null;
                Log.d(LOGTAG, "inputStreamReader close() ok");
            } catch (IOException e) {
                Log.e(LOGTAG, "IOException during inputStreamReader.close()", e);
            }
        }

        try {
            socket.close();
            Log.d(LOGTAG, "socket close() ok");
        } catch (IOException e) {
            Log.e(LOGTAG, "IOException during socket.close()", e);
        }
    }

    private String getThreadId() {
        return "(mac: " + macAddress + ")";
    }
}
