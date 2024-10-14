
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class Customer implements Runnable {
    private Bakery bakery;
    private Random rnd;
    private List<BreadType> shoppingList;
    private int shopTime;
    private int checkoutTime;
    private CountDownLatch doneSignal;
    
    /**
     * Initialize a customer object and randomize its shopping list
     */
    public Customer(Bakery bakery, CountDownLatch l) {
        this.bakery = bakery;
        this.rnd = new Random();
        this.shoppingList = new ArrayList<BreadType>();
        this.fillShoppingList();
        this.shopTime = 5 + this.rnd.nextInt(6); // felt reasonable: 5-10 ms
        this.checkoutTime = 5 + this.rnd.nextInt(6); // felt reasonable: 5-10 ms
        this.doneSignal = l;
    }

    /**
     * Run tasks for the customer
     */
    public void run() {

        System.out.printf("Enter %s\n", toString());

        for (BreadType bread : this.shoppingList) {

            Semaphore shelf;
            if(bread == BreadType.RYE){
                shelf = this.bakery.ryeShelf;
            } else if (bread == BreadType.SOURDOUGH){
                shelf = this.bakery.sourdoughShelf;
            } else {
                shelf = this.bakery.wonderShelf;
            }

            try {
                Thread.sleep(this.shopTime);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            try {
            shelf.acquire();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            this.bakery.takeBread(bread);
            shelf.release();

            System.out.printf("Customer %d took %s.\n", hashCode(), bread.toString());

        }

        float cost = getItemsValue();

        try {
            this.bakery.cashier.acquire();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        try {
            Thread.sleep(this.checkoutTime);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        System.out.printf("Customer %d purchased $%.2f of bread.\n", hashCode(), cost);

        try {
            this.bakery.sale.acquire();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        this.bakery.addSales(cost);
        this.bakery.sale.release();

        this.bakery.cashier.release();

        System.out.printf("Customer %d left.\n", hashCode());
        this.doneSignal.countDown();

    }

    /**
     * Return a string representation of the customer
     */
    public String toString() {
        return "Customer " + hashCode() + ": shoppingList=" + Arrays.toString(shoppingList.toArray()) + ", shopTime=" + shopTime + ", checkoutTime=" + checkoutTime;
    }

    /**
     * Add a bread item to the customer's shopping cart
     */
    private boolean addItem(BreadType bread) {
        // do not allow more than 3 items, chooseItems() does not call more than 3 times
        if (shoppingList.size() >= 3) {
            return false;
        }
        shoppingList.add(bread);
        return true;
    }

    /**
     * Fill the customer's shopping cart with 1 to 3 random breads
     */
    private void fillShoppingList() {
        int itemCnt = 1 + rnd.nextInt(3);
        while (itemCnt > 0) {
            addItem(BreadType.values()[rnd.nextInt(BreadType.values().length)]);
            itemCnt--;
        }
    }

    /**
     * Calculate the total value of the items in the customer's shopping cart
     */
    private float getItemsValue() {
        float value = 0;
        for (BreadType bread : shoppingList) {
            value += bread.getPrice();
        }
        return value;
    }
}
