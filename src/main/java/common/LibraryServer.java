package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Java RMI interface definition for the server with the specified operations
 * This interface declares business methods to be invoked remotely by RMI
 * clients (both managers and users)
 */
public interface LibraryServer extends Remote {

    int PORT = 1099;
    String URL_CON = "rmi://localhost:" + PORT + "//concordia";
    String URL_MCG = "rmi://localhost:" + PORT + "//mcgill";
    String URL_MON = "rmi://localhost:" + PORT + "//montreal";

    String getNextUserId(char userManager) throws RemoteException;
    WaitingUser checkWaitingQueue(String userId) throws RemoteException;
    List<Item> findItemLocally(String itemName) throws RemoteException;
    Item getItemLocally(String itemId) throws RemoteException;

    String addItem(String managerId, String itemId, String itemName, int quantity) throws RemoteException;
    String removeItem(String managerId, String itemId, int quantity) throws RemoteException;
    List<Item> listItemAvailability(String managerId) throws RemoteException;
    boolean borrowItem(String userId, String itemId, int numberOfDays) throws RemoteException;
    boolean addToWaitingList(String userId, String itemId, int numberOfDays) throws RemoteException;
    boolean removeFromWaitingList(WaitingUser waiting) throws RemoteException;
    List<Item> showAllItems() throws RemoteException;
    List<Item> findItem(String userId, String itemName) throws RemoteException;
    boolean returnItem(String userId, String itemId) throws RemoteException;
}
