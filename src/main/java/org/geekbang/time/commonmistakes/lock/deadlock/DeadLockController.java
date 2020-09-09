package org.geekbang.time.commonmistakes.lock.deadlock;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@RestController
@RequestMapping("deadlock")
@Slf4j

/**
 * 死锁问题
 * 在程序逻辑中对多个资源进行加锁时，容易产生死锁问题。
 * 该案例的原因是因为扣减库存的顺序不同，导致并发的情况下多个线程可能持有部分商品的锁，又等待其他线程释放另一部分的锁，于是出现死锁问题
 */
public class DeadLockController {

    private ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<>();

    public DeadLockController() {
        IntStream.range(0, 10).forEach(i -> items.put("item" + i, new Item("item" + i)));
    }

    private boolean createOrder(List<Item> order) {
        List<ReentrantLock> locks = new ArrayList<>();

        for (Item item : order) {
            try {
                if (item.lock.tryLock(10, TimeUnit.SECONDS)) {
                    locks.add(item.lock);
                } else {
                    locks.forEach(ReentrantLock::unlock);
                    return false;
                }
            } catch (InterruptedException e) {
            }
        }
        try {
            order.forEach(item -> item.remaining--);
        } finally {
            locks.forEach(ReentrantLock::unlock);
        }
        return true;
    }

    private List<Item> createCart() {
        return IntStream.rangeClosed(1, 3)
                .mapToObj(i -> "item" + ThreadLocalRandom.current().nextInt(items.size()))
                .map(name -> items.get(name)).collect(Collectors.toList());
    }

    /**
     * 获取订单的时候没有进行排序，导致库存的扣减顺序不一样，很容易造成死锁问题
     * @return
     */
    @GetMapping("wrong")
    public long wrong() {
        long begin = System.currentTimeMillis();
        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i -> {
                    List<Item> cart = createCart();
                    return createOrder(cart);
                })
                .filter(result -> result)
                .count();
        log.info("success:{} totalRemaining:{} took:{}ms items:{}",
                success,
                items.entrySet().stream().map(item -> item.getValue().remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin, items);
        return success;
    }

    /**
     * 获取订单之后进行排序，保证所有订单物品顺序一样
     * @return
     */
    @GetMapping("right")
    public long right() {
        long begin = System.currentTimeMillis();
        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i -> {
                    List<Item> cart = createCart().stream()
                            .sorted(Comparator.comparing(Item::getName))
                            .collect(Collectors.toList());
                    return createOrder(cart);
                })
                .filter(result -> result)
                .count();
        log.info("success:{} totalRemaining:{} took:{}ms items:{}",
                success,
                items.entrySet().stream().map(item -> item.getValue().remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin, items);
        return success;
    }

    @Data
    @RequiredArgsConstructor
    static class Item {
        final String name;
        int remaining = 1000;
        @ToString.Exclude
        ReentrantLock lock = new ReentrantLock();
    }
}
