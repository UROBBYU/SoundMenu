package org.urobbyu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Little <b>SoundVolumeView</b> data receiver
 */
public class InputDecoder {
    private String[] data;
    private String[] propNames;

    public static int iName;
    public static int iProcessID;
    public static int iProcessPath;
    public static int iItemID;
    public static int iDirection;
    public static int iType;
    public static int iDeviceName;
    public static int iDeviceState;

    InputDecoder() {
        reload();
    }

    /**
     * Reloads <b>data</b>
     * @return this object
     */
    public InputDecoder reload() {
        try {
            data = (new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(new String[]{ "cmd", "/c", "SoundVolumeView.exe /stab" }).getInputStream(), StandardCharsets.UTF_16LE))).lines().toArray(String[]::new);
            propNames = getRow(0);
            data = Arrays.copyOfRange(data, 1, data.length - 1);

            iProcessID = getPropertyIndex("Process ID");
            iProcessPath = getPropertyIndex("Process Path");
            iItemID = getPropertyIndex("Item ID");
            iDirection = getPropertyIndex("Direction");
            iType = getPropertyIndex("Type");
            iDeviceName = getPropertyIndex("Device Name");
            iDeviceState = getPropertyIndex("Device State");
            iName = getPropertyIndex("\uFEFFName");

        } catch (IOException e) { e.printStackTrace(); }

        return this;
    }

    /**
     * Retrieves one row from <b>data</b> by it's <b>index</b>
     * @param index index
     * @return data
     */
    public String[] getRow(int index) {
        return data[index].split("\t", -1);
    }

    /**
     * Retrieves one column from <b>data</b> by it's <b>property name</b>
     * @param prop name of the property
     * @return data
     */
    public String[] getColumn(String prop) {
        return getColumn(getPropertyIndex(prop));
    }

    /**
     * Retrieves one column from <b>data</b> by it's <b>index</b>
     * @param index index
     * @return data
     */
    public String[] getColumn(int index) {
        String[] ret = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            ret[i] = getRow(i)[index];
        }
        return ret;
    }

    /**
     * Searches for provided <b>property name</b>
     * @param prop property name
     * @return index
     */
    public int getPropertyIndex(String prop) {
        int ret = -1;
        for (int i = 0; i < rowSize(); i++) {
            if (prop.equals(propNames[i])) {
                ret = i;
                break;
            }
        }
        return (ret == rowSize() ? -1 : ret);
    }

    /**
     * Retrieves one <b>cell</b> from <b>data</b> by it's <b>X</b> and <b>property name</b>
     * @param row  X
     * @param prop property name
     * @return data
     */
    public String getCell(int row, String prop) {
        return getRow(row)[getPropertyIndex(prop)];
    }

    /**
     * Retrieves one <b>cell</b> from <b>data</b> by it's <b>X</b> and <b>Y</b>
     * @param row X
     * @param col Y
     * @return data
     */
    public String getCell(int row, int col) {
        return getRow(row)[col];
    }

    /**
     * Retrieves <b>size</b> of property list
     * @return size
     */
    public int rowSize() {
        return propNames.length;
    }

    /**
     * Retrieves <b>amount</b> of all entries
     * @return amount
     */
    public int size() {
        return data.length;
    }
}
