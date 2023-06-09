import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
public class Main {
    private static final int MIN_FLOOR = 1;
    private static final int MAX_FLOOR = 20;
    private static final int NUM_ELEVATORS = 2;
    private static final long GENERATOR_INTERVAL_MS = 2000;

    private static BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private static ExecutorService elevatorExecutor = Executors.newFixedThreadPool(NUM_ELEVATORS);

    public static void main(String[] args) {
        Thread managerThread = new Thread(Main::runManager);
        Thread generatorThread = new Thread(Main::runGenerator);

        managerThread.start();
        generatorThread.start();

        // Ждем, пока все потоки завершатся
        try {
            managerThread.join();
            generatorThread.join();
            elevatorExecutor.shutdown();
            elevatorExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static AtomicInteger nextElevatorIndex = new AtomicInteger(0);

    private static void runManager() {
        Elevator[] elevators = new Elevator[NUM_ELEVATORS];
        for (int i = 0; i < NUM_ELEVATORS; i++) {
            elevators[i] = new Elevator();
        }

        while (true) {
            try {
                Request request = requests.take();
                int elevatorIndex = nextElevatorIndex.getAndIncrement() % NUM_ELEVATORS;
                Elevator elevator = elevators[elevatorIndex];
                elevator.addRequest(request);

                elevatorExecutor.submit(() -> runElevator(elevator));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runGenerator() {
        Random random = new Random();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            int callingFloor = random.nextInt(MIN_FLOOR, MAX_FLOOR + 1);
            Direction direction = random.nextBoolean() ? Direction.UP : Direction.DOWN;
            Request request = new Request(callingFloor, direction);
            requests.add(request);
            System.out.println("New request: " + request.getCallingFloor() + " " + request.getDirection());

            try {
                Thread.sleep(GENERATOR_INTERVAL_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runElevator(Elevator elevator) {
        while (true) {
            Request request = elevator.getNextRequest();
            if (request == null) {
                continue; // Если нет запросов, переходим к следующей итерации
            }

            int currentFloor = elevator.getCurrentFloor();
            int targetFloor = request.getCallingFloor();
            Direction direction = request.getDirection();

            if (currentFloor < targetFloor) {
                for (int i = currentFloor + 1; i <= targetFloor; i++) {
                    elevator.setCurrentFloor(i);
                    System.out.println("Elevator " + elevator.getId() + " moving UP to floor " + i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (currentFloor > targetFloor) {
                for (int i = currentFloor - 1; i >= targetFloor; i--) {
                    elevator.setCurrentFloor(i);
                    System.out.println("Elevator " + elevator.getId() + " moving DOWN to floor " + i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Elevator " + elevator.getId() + " arrived at floor " + targetFloor);
            elevator.removeRequest(request);
        }
    }

    private static Elevator findClosestElevator(Request request, Elevator[] elevators) {
        int callingFloor = request.getCallingFloor();
        Elevator closestElevator = elevators[0];
        int minDistance = Math.abs(callingFloor - closestElevator.getCurrentFloor());

        for (int i = 1; i < NUM_ELEVATORS; i++) {
            Elevator elevator = elevators[i];
            int distance = Math.abs(callingFloor - elevator.getCurrentFloor());
            if (distance < minDistance) {
                closestElevator = elevator;
                minDistance = distance;
            }
        }

        return closestElevator;
    }
}

class Elevator {
    private static int nextId = 1;

    private int id;
    private int currentFloor;
    private BlockingQueue<Request> requests = new LinkedBlockingQueue<>();

    public Elevator() {
        this.id = nextId++;
    }

    public int getId() {
        return id;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public void addRequest(Request request) {
        requests.add(request);
    }

    public Request getNextRequest() {
        try {
            return requests.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void removeRequest(Request request) {
        requests.remove(request);
    }
}

enum Direction {
    UP,
    DOWN
}

class Request {
    private int callingFloor;
    private Direction direction;

    public Request(int callingFloor, Direction direction) {
        this.callingFloor = callingFloor;
        this.direction = direction;
    }

    public int getCallingFloor() {
        return callingFloor;
    }

    public Direction getDirection() {
        return direction;
    }
}
