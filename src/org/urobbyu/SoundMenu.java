package org.urobbyu;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import static org.urobbyu.InputDecoder.*;

/**
 * Sound Menu program
 * @author UROBBYU
 * @version 1.6
 * @since 1.0
 */
public class SoundMenu {
    private static final Properties favorites = new Properties();

    private static final Refresher refresher = new Refresher();

    private static final PopupMenu popup = new PopupMenu();

    private static final Menu appsMenu = new Menu("Apps");
    private static final Menu favoritesMenu = new Menu("Favorites");
    private static final MenuItem refreshItem = new MenuItem("(Refresh)");
    private static final MenuItem settingsItem = new MenuItem("Sound Settings");
    private static final MenuItem soundVolumeViewItem = new MenuItem("SoundVolumeView");
    private static final MenuItem exitItem = new MenuItem("Exit");

    private static final MenuItem editFavoriteItem = new MenuItem("Edit");
    private static final MenuItem clearFavoriteItem = new MenuItem("Remove All");

    private static boolean isEditMode = false;
    private static final InputDecoder inputDecoder = new InputDecoder();

    /**
     * Start of the program
     * @param args console arguments
     */
    public static void main(String[] args) throws AWTException, IOException {
        try {
            favorites.load(new FileInputStream("config.properties"));
        } catch (IOException ignored) {}

        // Checking if system supports the whole thing i'm trying to do
        if (!SystemTray.isSupported()) return;

        // Loading icon image
        BufferedImage img = ImageIO.read(ClassLoader.getSystemResource("icon.png"));

        // Getting SystemTray instance
        SystemTray systemTray = SystemTray.getSystemTray();

        // Building icon menu

        refreshAppsMenu();

        popup.add(favoritesMenu);
        popup.addSeparator();
        popup.add(appsMenu);
        popup.addSeparator();
        popup.add(refreshItem);
        popup.addSeparator();
        popup.add(settingsItem);
        popup.add(soundVolumeViewItem);
        popup.addSeparator();
        popup.add(exitItem);

        assert img != null;
        TrayIcon trayIcon = new TrayIcon(img.getScaledInstance(Math.round(16 * Toolkit.getDefaultToolkit().getScreenResolution() / 96f), -1, Image.SCALE_SMOOTH), "Sound Menu", popup);

        trayIcon.setImageAutoSize(true);

        // Setting up action handlers
        refreshItem.addActionListener(e -> refreshAppsMenu());
        settingsItem.addActionListener(e -> openApp(true));
        soundVolumeViewItem.addActionListener(e -> openApp(false));
        exitItem.addActionListener(e -> {
            refresher.doRun = false;
            refresher.interrupt();
            try {
                favorites.store(new FileOutputStream("config.properties"), null);
            } catch (IOException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
            systemTray.remove(trayIcon);
        });
        clearFavoriteItem.addActionListener(e -> {
            favorites.clear();
            refreshAppsMenu();
        });
        editFavoriteItem.addActionListener(SoundMenu::switchEditMode);

        // Adding my icon program to system tray
        systemTray.add(trayIcon);

        // Setting automatic application list refresher to refresh it every 30 seconds
        refresher.start();
    }

    /**
     * Turns edit mode on and off
     * @param e unused
     */
    private static void switchEditMode(ActionEvent e) {
        if (isEditMode) editFavoriteItem.setLabel("Edit");
        else editFavoriteItem.setLabel("Cancel");
        isEditMode = !isEditMode;

        refreshAppsMenu();
    }

    /**
     * Refills <b>appsMenu</b> and <b>favoritesMenu</b>
     */
    private static void refreshAppsMenu() {
        List<String> processNames = new ArrayList<>();
        List<String> processIds = new ArrayList<>();
        List<String> processPaths = new ArrayList<>();
        List<String> processDevices = new ArrayList<>();
        List<String> deviceNames = new ArrayList<>();
        List<String> deviceIds = new ArrayList<>();
        List<String> deviceSubNames = new ArrayList<>();

        favoritesMenu.removeAll();
        appsMenu.removeAll();
        inputDecoder.reload();

        // Getting list of apps
        for (String[] comb : getAppList()) {
            String id = comb[1];
            int index;
            if ((index = processIds.indexOf(id)) != -1) {
                processDevices.set(index, "undefined");
            } else {
                processNames.add(comb[0]);
                processIds.add(id);
                processPaths.add(comb[2]);
                processDevices.add(comb[3].split("\\|")[0]);
            }
        }

        // Getting list of devices
        for (String[] comb : getDeviceList()) {
            deviceNames.add(comb[0]);
            deviceIds.add(comb[1]);
            deviceSubNames.add(comb[2]);
        }

        // Filling Apps Menu
        for (int i = 0; i < processNames.size(); i++) {
            final int pI = i;
            Menu appMenu = new Menu(processNames.get(i) + " (" + processIds.get(i) + ")");
            Menu devicesMenu = new Menu("Output Device");
            CheckboxMenuItem muteFlag = new CheckboxMenuItem("Mute");

            muteFlag.addItemListener(e -> muteApp(processIds.get(pI), e.getStateChange()));

            for (int j = 0; j < deviceNames.size(); j++) {
                final int dI = j;
                MenuItem deviceItem = new MenuItem(deviceNames.get(j) + " (" + deviceSubNames.get(j) + ")");

                deviceItem.setEnabled(!(processDevices.get(i).equals(deviceIds.get(j))));

                deviceItem.addActionListener(e -> {
                    if (isEditMode) {
                        boolean flag = true;
                        for (int c = 1; c <= favorites.size() / 2; c++) {
                            if (favorites.getProperty("app" + c).equals(processPaths.get(pI)) && favorites.getProperty("device" + c).equals(deviceIds.get(dI))) flag = false;
                        }

                        if (flag) {
                            int index = favorites.size() / 2 + 1;
                            favorites.setProperty("app" + index, processPaths.get(pI));
                            favorites.setProperty("device" + index, deviceIds.get(dI));
                        }

                        (new Thread(() -> { try { Thread.sleep(200); } catch (InterruptedException ignored) { } refreshAppsMenu(); })).start();
                    } else
                        switchDevice(processIds.get(pI), deviceIds.get(dI));
                });

                devicesMenu.add(deviceItem);
            }

            appMenu.add(devicesMenu);
            appMenu.addSeparator();
            appMenu.add(muteFlag);
            appsMenu.add(appMenu);
        }

        // Refilling Favorites Menu
        List<String> addedMenus = new ArrayList<>();
        List<Object> keys = Collections.list(favorites.keys());
        List<Object> values = Collections.list(favorites.elements());

        for (int i = 1; i <= favorites.size() / 2; i++) {
            Menu favoriteAppMenu;
            MenuItem favoriteDeviceItem = new MenuItem();
            String appPath = (String) values.get(keys.indexOf("app" + i));
            String appDevice = (String) values.get(keys.indexOf("device" + i));

            // Adding Favorite App Menu
            int appIndex = processPaths.indexOf(appPath);
            if (addedMenus.contains(appPath)) {
                favoriteAppMenu = (Menu) favoritesMenu.getItem(addedMenus.indexOf(appPath));
            } else {
                favoriteAppMenu = new Menu();

                if (appIndex == -1) {
                    String[] filePath = appPath.split("\\\\");
                    favoriteAppMenu.setLabel(filePath[filePath.length - 1]);
                    favoriteAppMenu.setEnabled(false);
                } else
                    favoriteAppMenu.setLabel(processNames.get(appIndex));
                favoriteAppMenu.setName(appPath);
                addedMenus.add(appPath);
                favoritesMenu.add(favoriteAppMenu);
            }

            // Adding Device Menu Item
            int deviceIndex = deviceIds.indexOf(appDevice);

            if (deviceIndex == -1) {
                favoriteDeviceItem.setLabel("Device is not found");
                favoriteDeviceItem.setEnabled(false);
            } else {
                favoriteDeviceItem.setLabel(deviceNames.get(deviceIndex) + " (" + deviceSubNames.get(deviceIndex) + ")");
                final int index = i;
                if (appIndex != -1) favoriteDeviceItem.addActionListener(e -> {
                    if (isEditMode) {
                        removeFavProperty(index);

                        (new Thread(() -> { try { Thread.sleep(200); } catch (InterruptedException ignored) { } refreshAppsMenu(); })).start();
                    } else
                        switchDevice(processIds.get(appIndex), deviceIds.get(deviceIndex));
                });
            }

            favoriteAppMenu.add(favoriteDeviceItem);
        }

        if (isEditMode) {
            for (int i = 0; i < favoritesMenu.getItemCount(); i++) {
                MenuItem deleteFavoriteAppItem = new MenuItem("Delete All");
                Menu curFavMenu = (Menu) favoritesMenu.getItem(i);

                curFavMenu.add(deleteFavoriteAppItem);

                deleteFavoriteAppItem.addActionListener(e -> {
                    String appPath = curFavMenu.getName();

                    for (int index = 1; index <= favorites.size() / 2; index++) {
                        if (favorites.getProperty("app" + index).equals(appPath)) {
                            removeFavProperty(index);
                            index--;
                        }
                    }

                    (new Thread(() -> { try { Thread.sleep(200); } catch (InterruptedException ignored) { } refreshAppsMenu(); })).start();
                });
            }
        }

        // Adding Edit and Remove All Menu Items
        favoritesMenu.addSeparator();
        favoritesMenu.add(editFavoriteItem);
        favoritesMenu.add(clearFavoriteItem);
    }

    /**
     * Removes element from favorites by it's index
     * @param index index
     */
    private static void removeFavProperty(int index) {
        for (int j = index; j < favorites.size() / 2; j++) {
            favorites.setProperty("app" + j, favorites.getProperty("app" + (j + 1)));
            favorites.setProperty("device" + j, favorites.getProperty("device" + (j + 1)));
        }

        int n = favorites.size() / 2;

        favorites.remove("app" + n);
        favorites.remove("device" + n);
    }

    /**
     * Filtering <b>InputDecoder</b> to get only applications
     * @return list of application's properties
     */
    private static List<String[]> getAppList() {
        List<String[]> ret = new ArrayList<>();

        for (int i = 0; i < inputDecoder.size(); i++) {
            String[] row = inputDecoder.getRow(i);
            if (row[iDirection].equals("Render") && row[iType].equals("Application") && !row[iProcessID].equals("")) {
                ret.add(new String[]{row[iName], row[iProcessID], row[iProcessPath], row[iItemID]});
            }
        }

        return ret;
    }

    /**
     * Filtering <b>InputDecoder</b> to get only devices
     * @return list of device's properties
     */
    private static List<String[]> getDeviceList() {
        List<String[]> ret = new ArrayList<>();

        for (int i = 0; i < inputDecoder.size(); i++) {
            String[] row = inputDecoder.getRow(i);
            if (row[iDirection].equals("Render") && row[iType].equals("Device") && row[iDeviceState].equals("Active")) {
                ret.add(new String[]{row[iName], row[iItemID], row[iDeviceName]});
            }
        }

        return ret;
    }

    /**
     * Changes current playback device for the app with provided <b>processID</b>
     * @param processID system ID of the process
     * @param deviceID  ID of the sound output device
     */
    private static void switchDevice(String processID, String deviceID) {
        try {
            Runtime.getRuntime().exec("SoundVolumeView.exe /SetAppDefault " + deviceID + " all " + processID);
        } catch (IOException e) {
            e.printStackTrace();
        }

        (new Thread(() -> { try { Thread.sleep(200); } catch (InterruptedException ignored) { } refreshAppsMenu(); })).start();
    }

    /**
     * Mutes and unmutes the app with provided <b>processID</b>
     * @param processID system ID of the process
     * @param state     1 - Mute, 2 - Unmute
     */
    private static void muteApp(String processID, int state) {
        try {
            Runtime.getRuntime().exec("SoundVolumeView.exe /" + (state == ItemEvent.SELECTED ? "M" : "Unm") + "ute " + processID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens <b>System Apps Volume Settings</b> or <b>SoundVolumeView App</b>
     * @param settings T - <b>System Apps Volume Settings</b>, F - <b>SoundVolumeView App</b>
     */
    private static void openApp(boolean settings) {
        try {
            if (settings)
                Runtime.getRuntime().exec("cmd /c start ms-settings:apps-volume");
            else
                Runtime.getRuntime().exec("SoundVolumeView.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Additional thread to refresh Apps list every 30 seconds
     */
    private static class Refresher extends Thread {
        public boolean doRun = true;

        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(30000);
                refreshAppsMenu();
                if (doRun) run();
            } catch (InterruptedException ignored) { }
        }
    }
}