package server;

import common.Item;
import common.WaitingUser;

import java.util.*;

/**
 * Repository for the library server. Contains all the HashMaps with data
 */
public class Library {

    /**
     * Library prefix (CON, MCG or MON)
     */
    private static String prefix;
    /**
     * All the books, contained in this library, key is a itemId
     */
    private HashMap<String, Item> books;
    /**
     * Waiting list for user, who tried to borrow a specific item, but it
     * wasn't available at the moment
     */
    private Queue<WaitingUser> waitingList;
    /**
     * Books, that were at the waiting list, but now they returned. Once the client
     * launches his application, it will automatically borrow books from this list
     */
    private List<WaitingUser> availableForLanding;
    /**
     * A numeric counter for generating next itemID
     */
    private int idCounter = 0;

    /**
     * Main constructor for Library repository
     * @param prefix Library prefix (CON, MCG or MON)
     */
    Library(String prefix) {
        books = new HashMap<>();
        waitingList = new LinkedList<>();
        availableForLanding = new ArrayList<>();
        this.prefix = prefix;
        if ("CON".equals(prefix)) {
            addItem(getPrefix() + String.format("%04d", getNextId()), "Hamlet by William Shakespeare", 1);
            addItem(getPrefix() + String.format("%04d", getNextId()), "War and Peace by Leo Tolstoy", 3);
            addItem(getPrefix() + String.format("%04d", getNextId()), "The Odyssey by Homer", 2);
            addItem(getPrefix() + String.format("%04d", getNextId()), "One Hundred Years of Solitude by Gabriel Garcia Marquez", 1);
            addItem(getPrefix() + String.format("%04d", getNextId()), "The Divine Comedy by Dante Alighieri", 4);
        } else if ("MCG".equals(prefix)) {
            addItem(getPrefix() + String.format("%04d", getNextId()), "Hamlet by William Shakespeare", 2);
            addItem(getPrefix() + String.format("%04d", getNextId()), "Don Quixote by Miguel de Cervantes", 1);
            addItem(getPrefix() + String.format("%04d", getNextId()), "Ulysses by James Joyce", 2);
            addItem(getPrefix() + String.format("%04d", getNextId()), "The Great Gatsby by F. Scott Fitzgerald", 4);
            addItem(getPrefix() + String.format("%04d", getNextId()), "Moby Dick by Herman Melville", 5);
        } else {
            addItem(getPrefix() + String.format("%04d", getNextId()), "Hamlet by William Shakespeare", 3);
            addItem(getPrefix() + String.format("%04d", getNextId()), "The Divine Comedy by Dante Alighieri", 3);
            addItem(getPrefix() + String.format("%04d", getNextId()), "The Brothers Karamazov by Fyodor Dostoyevsky", 1);
            addItem(getPrefix() + String.format("%04d", getNextId()), "Madame Bovary by Gustave Flaubert", 1);
            addItem(getPrefix() + String.format("%04d", getNextId()), "The Adventures of Huckleberry Finn by Mark Twain", 1);
        }
    }

    /**
     * Returns library prefix
     * @return CON, MCG or MON
     */
    static String getPrefix() {
        return prefix;
    }

    /**
     * returns counter for next itemId
     * @return counter incremented by 1
     */
    int getNextId() {
        return ++idCounter;
    }

    /**
     * Method attempts to add an item to the repository. Unless there is a book
     * with this id, but different title, the method succeeds
     * @param itemId book id
     * @param itemName book title
     * @param quantity how many to add
     * @return success or failure
     */
    String addItem(String itemId, String itemName, int quantity) {
        Item existing = books.get(itemId);
        String response;
        if (existing == null) {
            books.put(itemId, new Item(itemId, itemName, quantity));
            response = "OK: Successfully added";
        } else if (itemName.equals(existing.getItemName())) {
            existing.setQuantity(existing.getQuantity() + quantity);
            books.put(itemId, existing);
            response = "OK: Successfully added";
        } else
            response = "Error: book with id \"" + itemId + "\" exists, but has a different name";
        for (WaitingUser waiting : waitingList)
            if (waiting.getItemId().equals(itemId)) {
                waitingList.remove(waiting);
                availableForLanding.add(waiting);
                continue;
            }
        return response;
    }

    /**
     * Method attempts to remove this item from repository. Fails if there is no
     * such ID or if the requested quantity is grater than amount of books with this
     * id in the library
     * @param itemId book id
     * @param quantity how many to remove. -1 is for all the items with this id
     * @return message like "OK: ..." or "Error: ..."
     */
    String removeItem(String itemId, int quantity) {
        Item existing = books.get(itemId);
        if (existing == null)
            return "No such book in the library " + getPrefix();
        else if (quantity == -1) {
            books.remove(itemId);
            return "OK: Successfully removed all items";
        }
        // You should ensure that if the quantity of an item is zero, no more users cannot borrow that item
        else if (existing.getQuantity() < quantity)
            return "Not enough books of id " + itemId + " in the library";
        else {
            existing.setQuantity(existing.getQuantity() - quantity);
            books.put(itemId, existing);
            return "OK: Successfully removed " + quantity + " items";
        }
    }

    /**
     * Shows all id's that are kept in this library
     * @return list of books
     */
    List<Item> listItemAvailability() {
        return new ArrayList<>(books.values());
    }

    /**
     * Method adds user to a waiting list
     * @param userId id of the user
     * @param itemId id of the book user tries to borrow
     * @param numberOfDays number of days, will be used in method borrowItem, once this
     *                     item becomes available
     * @return success or failure
     */
    boolean addToWaitingList(String userId, String itemId, int numberOfDays) {
        WaitingUser user = new WaitingUser(userId, itemId, numberOfDays);
        waitingList.add(user);
        return true;
    }

    /**
     * Removes a record from a waiting list
     * @param user user id and book id
     * @return success or failure
     */
    boolean removeFromWaitingList(WaitingUser user) {
        for (WaitingUser u : availableForLanding)
            if (u.getUserId().equals(user.getUserId())
            && u.getItemId().equals(user.getItemId())) {
                availableForLanding.remove(u);
                return true;
            }
        return false;
    }

    /**
     * Returns a list of items, which title contains given string
     * @param itemName part of the title to search
     * @return list of items
     */
    List<Item> findItem(String itemName) {
        itemName = itemName.toLowerCase();
        List<Item> result = new ArrayList<>();
        for (Item item : books.values())
            if (item.getItemName().toLowerCase().contains(itemName))
                result.add(item);
        return result;
    }

    /**
     * Returns list of user-book records, that were in a waiting list, but
     * now are available for borrowing
     * @return list of user-book records
     */
    public List<WaitingUser> getAvailableForLanding() {
        return availableForLanding;
    }

    /**
     * Searches a book title and quantity by given itemId
     * @param itemId book to search
     * @return book title and quantity
     */
    Item getItem(String itemId) {
        return books.get(itemId);
    }
}
