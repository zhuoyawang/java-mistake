package org.geekbang.time.commonmistakes.lock.lockgranularity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RestController
@RequestMapping("lockgranularity")
@Slf4j

/**
 * 在加锁的时候，需要考虑锁的粒度和场景问题
 *
 * 通常情况下，60%的业务代码是三层结构，数据经过无状态的controller、service、Repository流转到数据库。没有必要使用synchronized来保护数据
 *
 * 可能会极大地降低性能。在使用spring框架时，默认情况下Controller、service、repository是单例的，
 * 加上synchronized会导致整个程序几乎就只能支持单线程，造成极大地性能问题
 *
 * 即使我们确实有一些共享资源需要保护，也要尽可能降低锁的粒度，仅对必要的代码块甚至是需要保护的资源本身加锁
 */
public class LockGranularityController {

    private List<Integer> data = new ArrayList<>();

    /**
     * 一个慢方法
     */
    private void slow() {
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    /**
     * 对慢方法和操作ArrayList同时加锁 -----会有比较大的耗时
     * @return
     */
    @GetMapping("wrong")
    public int wrong() {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            synchronized (this) {
                slow();
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

    /**
     * 只对ArrayList加锁。
     * @return
     */
    @GetMapping("right")
    public int right() {
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
            slow();
            synchronized (data) {
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

    /**
     * 如果精细化考虑了锁应用范围后，性能还无法满足的话，我们就要考虑另一个维度的粒度问题了，
     * 即：区分读写场景以及资源的访问冲突，考虑使用悲观方式的锁还是乐观方式的锁。
     */
}
