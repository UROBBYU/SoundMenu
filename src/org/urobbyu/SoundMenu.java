package org.urobbyu;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static org.urobbyu.InputDecoder.*;

/**
 * Sound Menu program
 * @author UROBBYU
 * @version 1.7
 * @since 1.0
 */
public class SoundMenu {
    private static final Properties favorites = new Properties();
    private static final Refresher refresher = new Refresher();
    private static final Shutdowner shutdowner = new Shutdowner();
    private static final PopupMenu mainPopup = new PopupMenu();
    private static final PopupMenu favPopup = new PopupMenu();

    private static final Menu appsMenu = new Menu("Apps");
    private static final Menu favoritesMenu = new Menu("Favorites");
    private static final MenuItem refreshItem = new MenuItem("(Refresh)");
    private static final MenuItem settingsItem = new MenuItem("Sound Settings");
    private static final MenuItem soundVolumeViewItem = new MenuItem("SoundVolumeView");
    private static final MenuItem exitItem = new MenuItem("Exit");
    private static final MenuItem editFavoriteItem = new MenuItem("Edit");
    private static final MenuItem clearFavoriteItem = new MenuItem("Remove All");

    private static final TrayIcon trayIcon = new TrayIcon(loadIcon("icon.png"), "Sound Menu", favPopup);

    private static final List<String> processNames = new ArrayList<>();
    private static final List<String> processMuted = new ArrayList<>();
    private static final List<String> processIds = new ArrayList<>();
    private static final List<String> processPaths = new ArrayList<>();
    private static final List<String> processDevices = new ArrayList<>();
    private static final List<String> deviceNames = new ArrayList<>();
    private static final List<String> deviceIds = new ArrayList<>();
    private static final List<String> deviceSubNames = new ArrayList<>();

    private static boolean noFav = false;
    private static boolean isFullMenu = false;
    private static boolean loadFav = true;
    private static boolean saveFav = true;
    private static boolean includeOptions = true;
    private static boolean doubleClickSwitch = true;
    private static boolean simpleMenu = false;
    private static boolean noInfo = false;
    private static boolean showHelp = false;

    private static boolean isEditMode = false;
    private static boolean isFavMode = true;

    private static final InputDecoder inputDecoder = new InputDecoder();

    /**
     * Start of the program
     * @param args console arguments
     */
    public static void main(String[] args) throws AWTException {
        try {
            parseArgs(args);

            if (showHelp) {
                showHelp();
                return;
            }

            if (loadFav) favorites.load(new FileInputStream("config.properties"));
        } catch (ParseException parseException) {
            System.out.println(parseException.getMessage());
            return;
        } catch (IOException ignored) {}

        // Checking if system supports the whole thing i'm trying to do
        if (!SystemTray.isSupported()) return;

        // Getting SystemTray instance
        SystemTray systemTray = SystemTray.getSystemTray();

        // Building icon menu
        refreshData();
        refreshAppsMenu();
        refreshFavorites();

        if (!noFav) {
            mainPopup.addSeparator();
            mainPopup.add(favoritesMenu);
        }

        mainPopup.addSeparator();
        mainPopup.add(appsMenu);
        mainPopup.addSeparator();
        mainPopup.add(refreshItem);

        if (includeOptions) {
            mainPopup.addSeparator();
            mainPopup.add(settingsItem);
            mainPopup.add(soundVolumeViewItem);
        }

        mainPopup.addSeparator();
        mainPopup.add(exitItem);
        mainPopup.addSeparator();

        trayIcon.setImageAutoSize(true);
        makeBold(exitItem, favoritesMenu, appsMenu, favPopup);
        makePlain(editFavoriteItem, clearFavoriteItem);

        // Setting up action handlers
        refreshItem.addActionListener(e -> {
            inputDecoder.reload();
            refreshData();
            refreshAppsMenu();
            refreshFavorites();
        });

        if (includeOptions) {
            settingsItem.addActionListener(e -> openApp(true));

            soundVolumeViewItem.addActionListener(e -> openApp(false));
        }

        Runtime.getRuntime().addShutdownHook(shutdowner);
        exitItem.addActionListener(shutdowner);

        clearFavoriteItem.addActionListener(e -> {
            favorites.clear();
            refreshFavorites();
        });

        editFavoriteItem.addActionListener(SoundMenu::switchEditMode);

        if (doubleClickSwitch)
            trayIcon.addActionListener(SoundMenu::switchFavMode);
        else
            trayIcon.addActionListener(shutdowner);

        if (isFullMenu) switchFavMode(null);

        // Adding my icon program to system tray
        systemTray.add(trayIcon);

        // Setting automatic application list refresher to refresh it every 30 seconds
        refresher.start();
    }

    /**
     * Parses program arguments
     * @param args arguments
     * @throws ParseException thrown if any argument is invalid
     */
    private static void parseArgs(String[] args) throws ParseException {
        List<String> s = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();

            if (arg.matches("--.+")) arg = arg.substring(2);
            else if (arg.matches("(/|-).+")) arg = arg.substring(1);
            else throw new ParseException("Invalid argument prefix: " + arg, i);

            switch (arg) {
                case "fm":
                case "nfl":
                case "nfs":
                case "nf":
                case "no":
                case "se":
                case "sm":
                case "ni":
                case "?":
                case "help":
                    if (!s.contains(arg)) s.add(arg);
                    break;
                default:
                    throw new ParseException("Invalid argument: " + arg, i);
            }
        }

        if (s.contains("?") || s.contains("help")) showHelp = true;

        if (s.contains("nf")) noFav = true;

        if (s.contains("nfl") || noFav) loadFav = false;

        if (s.contains("nfs") || noFav) saveFav = false;

        if (s.contains("se") || noFav) doubleClickSwitch = false;

        if (s.contains("fm") || noFav) isFullMenu = true;

        if (s.contains("no")) includeOptions = false;

        if (s.contains("sm")) simpleMenu = true;

        if (s.contains("ni")) noInfo = true;
    }

    /**
     * Shows help console message
     */
    private static void showHelp() {
        System.out.println(
            "\n/-------------------------------------------------------------\\\n" +
            "| start.cmd [-nf | (-nfl | -nfs | -se | -fm)] [-no]           |\n" +
            "|------------------------|Description|------------------------|\n" +
            "|    Little sound mapping java tray application.              |\n" +
            "|-----------------------|Argument List|-----------------------|\n" +
            "|    -nfl    Disables favorites file loading.                 |\n" +
            "|                                                             |\n" +
            "|    -nfs    Disables favorites file saving.                  |\n" +
            "|                                                             |\n" +
            "|    -se     Disables double-click Favorite Mode switching.   |\n" +
            "|            Instead it closes the application.               |\n" +
            "|                                                             |\n" +
            "|    -fm     Initially loads application in Full Mode.        |\n" +
            "|                                                             |\n" +
            "|    -nf     Disables favorites entirely.                     |\n" +
            "|                                                             |\n" +
            "|    -no     Hides 'Sound Settings' and 'SoundVolumeView'.    |\n" +
            "|                                                             |\n" +
            "|    -sm     Hides 'Mute' and 'Output Device' by entirely     |\n" +
            "|            skipping this menu.                              |\n" +
            "|                                                             |\n" +
            "|    -ni     Hides IDs of processes and SubNames of devices.  |\n" +
            "|-------------------------|Examples-|-------------------------|\n" +
            "|   start.cmd -nf                                             |\n" +
            "|   start.cmd /nf --no                                        |\n" +
            "|   start.cmd -nfl /nfs --se -fm /no                          |\n" +
            "\\-------------------------------------------------------------/\n"
        );
    }

    /**
     * Sets menu item's font style to bold
     * @param components menu items
     */
    private static void makeBold(MenuComponent... components) { for (MenuComponent c : components) c.setFont(new Font(Font.DIALOG, Font.BOLD, 12)); }

    /**
     * Sets menu item's font style to bold
     * @param components menu items
     */
    private static void makePlain(MenuComponent... components) { for (MenuComponent c : components) c.setFont(new Font(Font.DIALOG, Font.PLAIN, 12)); }

    /**
     * Loads an image from system resources
     */
    private static Image loadIcon(String filename) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(ClassLoader.getSystemResource(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert img != null;
        return img.getScaledInstance(Math.round(16 * Toolkit.getDefaultToolkit().getScreenResolution() / 96f), -1, Image.SCALE_SMOOTH);
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
        refreshFavorites();
    }

    /**
     * Turns favorite mode on and off
     * @param e unused
     */
    private static void switchFavMode(ActionEvent e) {
        if (isFavMode) {
            trayIcon.setImage(loadIcon("icon_orange.png"));
            trayIcon.setPopupMenu(mainPopup);
        }
        else {
            if (isEditMode) switchEditMode(null);
            trayIcon.setImage(loadIcon("icon.png"));
            trayIcon.setPopupMenu(favPopup);
        }
        isFavMode = !isFavMode;

        refreshFavorites();
    }

    /**
     * Processes data retrieved from <b>InputDecoder</b>
     */
    private static void refreshData() {
        processNames.clear();
        processMuted.clear();
        processIds.clear();
        processPaths.clear();
        processDevices.clear();
        deviceNames.clear();
        deviceIds.clear();
        deviceSubNames.clear();

        // Getting list of apps
        for (String[] comb : getAppList()) {
            String id = comb[2];
            int index;
            if ((index = processIds.indexOf(id)) != -1) {
                processDevices.set(index, "undefined");
            } else {
                processNames.add(comb[0]);
                processMuted.add(comb[1]);
                processIds.add(id);
                processPaths.add(comb[3]);
                processDevices.add(comb[4].split("\\|")[0]);
            }
        }

        // Getting list of devices
        for (String[] comb : getDeviceList()) {
            deviceNames.add(comb[0]);
            deviceIds.add(comb[1]);
            deviceSubNames.add(comb[2]);
        }
    }

    /**
     * Refills <b>appsMenu</b>
     */
    private static void refreshAppsMenu() {
        appsMenu.removeAll();

        appsMenu.addSeparator();

        for (int i = 0; i < processNames.size(); i++) {
            final int pI = i;
            Menu appMenu = new Menu(processNames.get(i) + (noInfo ? "" : " (" + processIds.get(i) + ")"));
            Menu devicesMenu = new Menu("Output Device");
            CheckboxMenuItem muteFlag = new CheckboxMenuItem("Mute", processMuted.get(i).equals("Yes"));

            makePlain(muteFlag);

            appMenu.addSeparator();
            if (!simpleMenu) devicesMenu.addSeparator();

            muteFlag.addItemListener(e -> muteApp(processIds.get(pI), e.getStateChange()));

            for (int j = 0; j < deviceNames.size(); j++) {
                final int dI = j;
                MenuItem deviceItem = new MenuItem(deviceNames.get(j) + (noInfo ? "" : " (" + deviceSubNames.get(j) + ")"));

                deviceItem.setEnabled(!(processDevices.get(i).equals(deviceIds.get(j))) || isEditMode);

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

                        refreshFavorites();
                    } else
                        switchDevice(processIds.get(pI), deviceIds.get(dI));
                });

                if (simpleMenu)
                    appMenu.add(deviceItem);
                else
                    devicesMenu.add(deviceItem);
            }

            if (!simpleMenu) {
                devicesMenu.addSeparator();
                appMenu.add(devicesMenu);
                appMenu.addSeparator();
                appMenu.add(muteFlag);
            }
            appMenu.addSeparator();
            appsMenu.add(appMenu);
        }
        appsMenu.addSeparator();
    }

    /**
     * Refills <b>favoritesMenu</b> and <b>favPopup</b>
     */
    private static void refreshFavorites() {
        if (!noFav) {
            favoritesMenu.removeAll();
            favPopup.removeAll();

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
                        favoriteAppMenu.setEnabled(isEditMode);
                    } else
                        favoriteAppMenu.setLabel(processNames.get(appIndex));
                    favoriteAppMenu.setName(appPath);
                    addedMenus.add(appPath);
                    if (favoritesMenu.getItemCount() > 0) favoritesMenu.addSeparator();
                    favoritesMenu.add(favoriteAppMenu);
                }

                // Adding Device Menu Item
                int deviceIndex = deviceIds.indexOf(appDevice);

                if (deviceIndex == -1) {
                    favoriteDeviceItem.setLabel("Device not found");
                    favoriteDeviceItem.setEnabled(false);
                } else {
                    favoriteDeviceItem.setLabel(deviceNames.get(deviceIndex) + (noInfo ? "" : " (" + deviceSubNames.get(deviceIndex) + ")"));
                    final int index = i;
                    favoriteDeviceItem.addActionListener(e -> {
                        if (isEditMode) {
                            removeFavProperty(index);

                            refreshFavorites();
                        } else
                            switchDevice(processIds.get(appIndex), deviceIds.get(deviceIndex));
                    });
                }

                if (favoriteAppMenu.getItemCount() > 0) favoriteAppMenu.addSeparator();
                favoriteAppMenu.add(favoriteDeviceItem);
            }

            for (int i = 0; i < favoritesMenu.getItemCount(); i++) {
                Menu curFavMenu = (Menu) favoritesMenu.getItem(i);

                if (isEditMode) {
                    MenuItem deleteFavoriteAppItem = new MenuItem("Delete All");

                    makePlain(deleteFavoriteAppItem);

                    curFavMenu.addSeparator();
                    curFavMenu.add(deleteFavoriteAppItem);

                    deleteFavoriteAppItem.addActionListener(e -> {
                        String appPath = curFavMenu.getName();

                        for (int index = 1; index <= favorites.size() / 2; index++) {
                            if (favorites.getProperty("app" + index).equals(appPath)) {
                                removeFavProperty(index);
                                index--;
                            }
                        }

                        refreshFavorites();
                    });
                }

                curFavMenu.insertSeparator(0);
                curFavMenu.addSeparator();
            }

            if (isFavMode) {
                // Refilling Favorites Popup
                favPopup.addSeparator();

                for (int i = 0; i < favoritesMenu.getItemCount(); i++) {
                    favPopup.add(favoritesMenu.getItem(i));
                }

                if (favPopup.getItemCount() == 1) {
                    MenuItem emptyItem = new MenuItem("Switch mode");

                    emptyItem.addActionListener(SoundMenu::switchFavMode);

                    favPopup.add(emptyItem);
                }

                favPopup.addSeparator();
            } else {
                // Adding Edit and Remove All Menu Items
                if (favoritesMenu.getItemCount() > 1) favoritesMenu.addSeparator();
                favoritesMenu.add(editFavoriteItem);
                favoritesMenu.add(clearFavoriteItem);
            }

            favoritesMenu.insertSeparator(0);
            favoritesMenu.addSeparator();
        }
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
                ret.add(new String[]{row[iName], row[iMuted], row[iProcessID], row[iProcessPath], row[iItemID]});
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

        refreshData();
        refreshAppsMenu();
        refreshFavorites();
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
                inputDecoder.reload();
                refreshData();
                refreshAppsMenu();
                refreshFavorites();
                if (doRun) run();
            } catch (InterruptedException ignored) { }
        }
    }

    /**
     * Thread that performs correct program exit
     */
    private static class Shutdowner extends Thread implements ActionListener {
        @Override
        public void run() {
            refresher.doRun = false;
            refresher.interrupt();
            if (saveFav)
            try {
                favorites.store(new FileOutputStream("config.properties"), null);
            } catch (IOException fileNotFoundException) {
                fileNotFoundException.printStackTrace();
            }
            SystemTray.getSystemTray().remove(trayIcon);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            start();
        }
    }
}