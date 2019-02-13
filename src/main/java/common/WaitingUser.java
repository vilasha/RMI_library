package common;

import java.io.Serializable;

/**
 * Entity class for users in a waiting list
 */
public class WaitingUser implements Serializable {
    private String userId;
    private String itemId;
    private int numberOfDays;

    public WaitingUser(String userId, String itemId, int numberOfDays) {
        this.userId = userId;
        this.itemId = itemId;
        this.numberOfDays = numberOfDays;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }
}
