
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.CountDownLatch;

public class Bakery implements Runnable {
    private static final int TOTAL_CUSTOMERS = 200; // 200
    private static final int CAPACITY = 50; // 50
    private static final int FULL_BREAD = 20;
    private Map<BreadType, Integer> availableBread;
    private ExecutorService executor;
    private float sales = 0;
    private CountDownLatch doneSignal = new CountDownLatch(TOTAL_CUSTOMERS);

    private static final int TOTAL_RYE_SHELVES = 1;
    private static final int TOTAL_SOURDOUGH_SHELVES = 1;
    private static final int TOTAL_WONDER_SHELVES = 1;
    private static final int TOTAL_CASHIERS = 4;

    public Semaphore ryeShelf = new Semaphore(Bakery.TOTAL_RYE_SHELVES);
    public Semaphore sourdoughShelf = new Semaphore(Bakery.TOTAL_SOURDOUGH_SHELVES);
    public Semaphore wonderShelf = new Semaphore(Bakery.TOTAL_WONDER_SHELVES);
    public Semaphore cashier = new Semaphore(Bakery.TOTAL_CASHIERS);
    public Semaphore sale = new Semaphore(1);


    /**
     * Remove a loaf from the available breads and restock if necessary
     */
    public void takeBread(BreadType bread) {
        int breadLeft = availableBread.get(bread);
        if (breadLeft > 0) {
            availableBread.put(bread, breadLeft - 1);
        } else {
            System.out.println("No " + bread.toString() + " bread left! Restocking...");
            // restock by preventing access to the bread stand for some time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            availableBread.put(bread, FULL_BREAD - 1);
        }
    }

    /**
     * Add to the total sales
     */
    public void addSales(float value) {
        sales += value;
    }

    /**
     * Run all customers in a fixed thread pool
     */
    public void run() {
        availableBread = new ConcurrentHashMap<BreadType, Integer>();
        availableBread.put(BreadType.RYE, FULL_BREAD);
        availableBread.put(BreadType.SOURDOUGH, FULL_BREAD);
        availableBread.put(BreadType.WONDER, FULL_BREAD);

        this.executor = Executors.newFixedThreadPool(Bakery.CAPACITY);
        for(int i = 0; i < Bakery.TOTAL_CUSTOMERS; i++){
            this.executor.execute(new Customer(this, this.doneSignal));
        }

        try{
            this.doneSignal.await();
            System.out.printf("Total sales = %.2f\n", this.sales);
            this.executor.shutdown();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
