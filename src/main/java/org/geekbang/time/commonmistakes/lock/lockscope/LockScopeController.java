package org.geekbang.time.commonmistakes.lock.lockscope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.IntStream;

@RestController
@RequestMapping("lockscope")
@Slf4j

/**
 * 加锁前要清楚锁和被保护的对象是不是一个层面的
 * 静态字段属于类，类级别的锁才可以保护，非静态字段字段属于类实例，实例级别的锁就可以保护。
 * 在使用锁之前一定要先清楚，我们保护的逻辑是什么，多线程执行的情况下应该如何保护。
 *
 */
public class LockScopeController {

    @GetMapping("wrong")
    public int wrong(@RequestParam(value = "count", defaultValue = "1000000") int count) {
        Data.reset();
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().wrong());
        return Data.getCounter();
    }

    @GetMapping("right")
    public int right(@RequestParam(value = "count", defaultValue = "1000000") int count) {
        Data.reset();
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().right());
        return Data.getCounter();
    }


    /**
     * 两个线程会交错执行add 和compare的业务逻辑，而业务逻辑不是原子性的操作
     * a++和b++会穿梭在compare中,a<b也不是原子性的操作，需要先取出A的操作，再取出B的操作，然后进行比较。
     * 所有的过程都不是原子性的，所以不能保证结果的正确性
     */
    @GetMapping("wrong2")
    public String wrong2() {
        Interesting interesting = new Interesting();
        new Thread(() -> interesting.add()).start();
        new Thread(() -> interesting.compare()).start();
        return "OK";
    }

    /**
     * 需要给两个方法全部都加上锁才可以
     */
    @GetMapping("right2")
    public String right2() {
        Interesting interesting = new Interesting();
        new Thread(() -> interesting.add()).start();
        new Thread(() -> interesting.compareRight()).start();
        return "OK";
    }
}
