package server;

import common.Item;
import common.LibraryServer;
import common.WaitingUser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Java RMI server for Distributed library management system.
 * Each library runs its own server, which is defined by library prefix
 * in ServerImpl constructor
 */
public class ServerImpl extends UnicastRemoteObject
                    implements LibraryServer {

    /**
     * Library prefix (CON, MCG or MON)
     */
    private String prefix;
    /**
     * Counter is used to give to every user and manager a unique ID
     */
    private int userCounter = 0;
    /**
     * Link to Library storage, which contains all the HashMaps with data
     * (analogy to a repository)
     */
    private Library library;

    /**
     * Logger, that will contain all received commands
     */
    private static Logger log;

    /**
     * Constructor for the RMI server
     * @param prefix Library prefix (CON, MCG or MON)
     * @throws RemoteException can be thrown by a parent class UnicastRemoteObject
     */
    ServerImpl(String prefix) throws RemoteException {
        super();
        this.prefix = prefix.toUpperCase();
        library = new Library(prefix);
        log = Logger.getLogger(ServerImpl.class.getName());
        initLogger(log);
    }

    /**
     * Starts log to file
     * @param log logger
     */
    private void initLogger(Logger log) {
        FileHandler logHandler;
        try {
            logHandler = new FileHandler("server_" + prefix + ".log", true);
            logHandler.setFormatter(new SimpleFormatter());
            log.addHandler(logHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Returns a unique identifier for this library, which contains library prefix,
     * indicator whether it's user or manager and a number from userCounter
     * @param userManager indicator whether it's user or manager
     * @return ID for a user or manager like CONM1111 for a manager and
     * CONU1111 for a user
     */
    @Override
    public String getNextUserId(char userManager) {
        userCounter++;
        return prefix + userManager + String.format("%04d", userCounter);
    }

    /**
     * Method checks if this user has any items he is waiting for are already
     * available. If they are, a method "borrowItem" will be invoked
     * @param userId user ID
     * @return first book this user is waiting
     */
    @Override
    public WaitingUser checkWaitingQueue(String userId) {
        List<WaitingUser> available = library.getAvailableForLanding();
        for (WaitingUser user : available)
            if (user.getUserId().equals(userId))
                return user;
        return null;
    }

    /**
     * When a manager invokes this method on the associated server (determined by the unique managerID
     * prefix), it attempts to add an item with the information passed, and inserts the record at the
     * appropriate location in the hash map. The server returns information to the manager whether the
     * operation was successful or not. If an item already exists, the new quantity entered should be
     * added to the current quantity of the item. If an item does not exist in the database, then
     * simply add it.
     *
     * This method is also used if a user returns an item to the library (see method returnItem)
     * @param managerId id of manager, who adds the item
     * @param itemId id of the book
     * @param itemName title of the book
     * @param quantity how many books to add
     * @return response like "OK: ..." or "Error: ..."
     */
    @Override
    public String addItem(String managerId, String itemId, String itemName, int quantity) {
        if (managerId == null || managerId.length() < 4
                // Your servers should ensure that a user can only perform a user operation and cannot
                // perform a manager operation and vice-versa
                || managerId.charAt(3) != 'M' || !managerId.substring(0, 3).toUpperCase().equals(prefix))
            return "Error: current user is not authorized to fulfill this operation";
        String result = library.addItem(itemId, itemName, quantity);
        log.info("Received a command to add an item, managerId = " + managerId
                + "; itemId = " + itemId + "; itemName = " + itemName + "; quantity = " + quantity
                + "\nResponse: " + result);
        return result;
    }

     /**
     * When invoked by a manager, the server associated with this manager (determined by the unique
     * managerID) searches in the hashmap to find and delete the item. There can be two cases of
     * deletion, first, if the manager wants to decrease the quantity of that item, second, if the
     * manager wants to completely remove the item from the library. Upon success or failure it
     * returns a message to the manager and the logs are updated with this information. If an item
     * does not exist, then obviously there is no deletion performed. Just in case that, if an item
     * exists and a user has borrowed it, then, delete the item and take the necessary actions.
     *
     * This method is alsow used when a user borrows an item from the library (see method borrowItem)
     * @param managerId id of manager, who removes the item
     * @param itemId id of the book
     * @param quantity how many books to add
     * @return response like "OK: ..." or "Error: ..."
     */
    @Override
    public String removeItem(String managerId, String itemId, int quantity) {
        if (managerId == null || managerId.length() < 4
                // Your servers should ensure that a user can only perform a user operation and cannot
                // perform a manager operation and vice-versa
                || managerId.charAt(3) != 'M' || !managerId.substring(0, 3).toUpperCase().equals(prefix))
            return "Error: current user is not authorized to fulfill this operation";
        String result = library.removeItem(itemId, quantity);
        log.info("Received a command to remove an item, managerId = " + managerId
                + "; itemId = " + itemId + "; quantity = " + quantity
                + "\nResponse: " + result);
        return result;
    }

    /**
     * When a manager invokes this method from his/her library through the associated server, that
     * library server finds out the names and quantities of each item available in the library
     * @param managerId id of manager, who requests the list
     * @return list of all books in the current library
     */
    @Override
    public List<Item> listItemAvailability(String managerId) {
        if (managerId == null || managerId.length() < 4
                || managerId.charAt(3) != 'M' || !managerId.substring(0, 3).toUpperCase().equals(prefix))
            return null;
        List<Item> result = library.listItemAvailability();
        log.info("Received a command to list all the items, managerId = " + managerId
                + "\nResponse: " + result.toString());
        return result;
    }

    /**
     * When a user invokes this method from his/her library through the server associated with this
     * user (determined by the unique userId prefix), it attempts to borrow the specified item.
     * If the item is from a different library, then the user’s library sends a RMI request to the
     * item’s library to borrow. If the operation was successful, borrow the item and decrement the
     * quantity for that item. Also, display an appropriate message to the user. If the borrow
     * operation is unsuccessful, ask the user if he/she wants to be added in the waiting queue.
     * If prompted no, method ends, otherwise add the userID to the queue corresponding to the
     * requested item and whenever the item is available again, automatically lend the item to the
     * first user in that queue
     * @param userId id of the user who invoked this method
     * @param itemId id of the book, user requested
     * @param numberOfDays number of days to borrow
     * @return successful or not is the borrowing operation
     */
    @Override
    public boolean borrowItem(String userId, String itemId, int numberOfDays) {
        boolean result = false;
        if (userId == null || userId.length() < 4
                // Your servers should ensure that a user can only perform a user operation and cannot
                // perform a manager operation and vice-versa
                || userId.charAt(3) != 'U')
            return false;
        if (itemId.substring(0, 3).toUpperCase().equals(prefix))
            result = (library.removeItem(itemId, 1).startsWith("OK"));
        else {
            try {
                String libPref = itemId.substring(0, 3).toUpperCase();
                System.out.println("libFrefix=" + libPref);
                LibraryServer anotherLibrary = (LibraryServer) Naming.lookup(
                        libPref.equals("CON") ? URL_CON :
                                libPref.equals("MCG") ? URL_MCG : URL_MON);
                result = anotherLibrary.borrowItem(userId, itemId, numberOfDays);
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                e.printStackTrace();
            }
        }
        log.info("Received a command to borrow an item, userId = " + userId
                + "; itemId = " + itemId + "; numberOfDays = " + numberOfDays
                + "\nResponse: " + String.valueOf(result));
        return result;
    }

    /**
     * Method is invoked if a user tried to borrow an item, but it wasn't available.
     * Client application asks user if he wants to wait in the waiting list. If he does,
     * this method adds him into the waiting list
     * @param userId id of the user
     * @param itemId id of the item user is waiting
     * @param numberOfDays number of days, will be used in method borrowItem, once this
     *                     item becomes available
     * @return success or failure of the action
     */
    @Override
    public boolean addToWaitingList(String userId, String itemId, int numberOfDays) {
        boolean result;
        if (userId == null || userId.length() < 4
                // Your servers should ensure that a user can only perform a user operation and cannot
                // perform a manager operation and vice-versa
                || userId.charAt(3) != 'U')
            return false;
        result = library.addToWaitingList(userId, itemId, numberOfDays);
        log.info("Received a command add a user to a waiting list, userId = " + userId
                + "; itemId = " + itemId + "; numberOfDays = " + numberOfDays
                + "\nResponse: " + String.valueOf(result));
        return result;
    }

    /**
     * If an item became available and user successfully borrowed it, user record
     * will be removed from the waiting list
     * @param waiting user id and item id
     * @return success or failure of the action
     */
    @Override
    public boolean removeFromWaitingList(WaitingUser waiting) {
        return library.removeFromWaitingList(waiting);
    }

    /**
     * Method shows all available books from all the libraries
     * @return list of all the existing items
     */
    @Override
    public List<Item> showAllItems() {
        List<Item> result = new ArrayList<>();
        try {
            LibraryServer anotherLibrary = (LibraryServer) Naming.lookup(URL_CON);
            result.addAll(anotherLibrary.listItemAvailability("CONM1"));
            anotherLibrary = (LibraryServer) Naming.lookup(URL_MCG);
            result.addAll(anotherLibrary.listItemAvailability("MCGM1"));
            anotherLibrary = (LibraryServer) Naming.lookup(URL_MON);
            result.addAll(anotherLibrary.listItemAvailability("MONM1"));
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
        log.info("Received a command to list all the items from all the libraries"
                + "\nResponse: " + result.toString());
        return result;
    }

    /**
     * When a user invokes this method from his/her library through the server associated with this user,
     * that library server gets all the itemIDs with the specified itemNameand the number of such items
     * available in each of the libraries and display them on the console.
     * @param userId id of the user who invoked this method
     * @param itemName title of the book or its part
     * @return list of all the items from all the libraries, that contain itemName
     */
    @Override
    public List<Item> findItem(String userId, String itemName) {
        List<Item> result = new ArrayList<>();
        try {
            LibraryServer anotherLibrary = (LibraryServer) Naming.lookup(URL_CON);
            result.addAll(anotherLibrary.findItemLocally(itemName));
            anotherLibrary = (LibraryServer) Naming.lookup(URL_MCG);
            result.addAll(anotherLibrary.findItemLocally(itemName));
            anotherLibrary = (LibraryServer) Naming.lookup(URL_MON);
            result.addAll(anotherLibrary.findItemLocally(itemName));
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            e.printStackTrace();
        }
        log.info("Received a command to find all the items from all the libraries " +
                "with a title contains \"" + itemName + "\"; userId = " + userId
                + "\nResponse: " + result.toString());
        return result;
    }

    /**
     * Method is only invoked by server application, it returns all the
     * items, which contain part of the name, on this server's library
     * @param itemName part of book's title
     * @return list of all the items in this library
     */
    public List<Item> findItemLocally(String itemName) {
        return library.findItem(itemName);
    }

    /**
     * When a user invokes this method from his/her library through the server associated with this user
     * (determined by the unique userID prefix) searches the hash map to find the itemID and returns
     * the item to its library. Upon success or failure it returns a message to the user and the logs are
     * updated with this information. It is required to check that an item can only be returned if it was
     * borrowed by the same user who sends the return request
     * @param userId id of the user who invoked this method
     * @param itemId id of the book, user returns
     * @return successful or not is the returning operation
     */
    @Override
    public boolean returnItem(String userId, String itemId) {
        boolean result = false;
        if (userId == null || userId.length() < 4
                // Your servers should ensure that a user can only perform a user operation and cannot
                // perform a manager operation and vice-versa
                || userId.charAt(3) != 'U')
            return false;
        if (itemId.substring(0, 3).toUpperCase().equals(prefix)) {
            Item item = library.getItem(itemId);
            result = library.addItem(itemId, item.getItemName(), 1).startsWith("OK");
            System.out.println("itemN = " + item.getItemName() + "; result = " + result);
        } else {
            try {
                String libPref = itemId.substring(0, 3).toUpperCase();
                LibraryServer anotherLibrary = (LibraryServer) Naming.lookup(
                        libPref.equals("CON") ? URL_CON :
                        libPref.equals("MCG") ? URL_MCG : URL_MON);
                Item item = anotherLibrary.getItemLocally(itemId);
                result = anotherLibrary.addItem(libPref + "M1", itemId, item.getItemName(), 1).startsWith("OK");
            } catch (NotBoundException | MalformedURLException | RemoteException e) {
                e.printStackTrace();
            }
        }
        log.info("Received a command to return an item, userId = " + userId
                + "; itemId = " + itemId
                + "\nResponse: " + String.valueOf(result));
        return result;
    }

    /**
     * Method is only invoked by server application, it returns book title and quantity
     * by given item id
     * @param itemId id of the item to search
     * @return book title and quantity
     */
    public Item getItemLocally(String itemId) {
        return library.getItem(itemId);
    }
}
