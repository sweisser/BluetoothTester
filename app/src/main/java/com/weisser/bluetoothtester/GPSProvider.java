package com.weisser.bluetoothtester;

/**
 * Common interface for internal and external GPS providers.
 *
 *
 *
 * Created by stefan on 21/12/2016.
 */
public interface GPSProvider {
    int SUCCESS = 0;
    int ERR_NO_ADAPTER = 1;
    int ERR_NO_DEVICE = 2;
    int ERR_NO_SOCKET = 3;
    int ERR_SOCKET_CONNECT = 4;
    int ERR_SOCKET_INPUT_STREAM = 5;
    int ERR_NO_UUIDS = 6; // Fix: Turn bluetooth on, if it is off
}
