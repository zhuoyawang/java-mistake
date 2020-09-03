package org.geekbang.time.commonmistakes.concurrenttool.concurrenthashmapmisuse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;


/**
 * concurrentHashMap只能保证提供的原子性读写操作是线程安全的
 */
@RestController
@RequestMapping("concurrenthashmapmisuse")
@Slf4j
public class ConcurrentHashMapMisuseController {

    private static int THREAD_COUNT = 10;
    private static int ITEM_COUNT = 1000;


    /**
     * rangeClosed(begin,end),输出从begin到end（包括begin与end），间隔为1的数
     * range(begin,end),输出从begin到end（包括begin，不包括end），间隔为1的数
     *
     * boxed 将基本类型流变为其包装器流
     *
     */
    private ConcurrentHashMap<String, Long> getData(int count) {
        return LongStream.rangeClosed(1, count)
                .boxed()
                .collect(Collectors.toConcurrentMap(i -> UUID.randomUUID().toString(), Function.identity(),
                        (o1, o2) -> o1, ConcurrentHashMap::new));
    }


    /**
     * 在使用concurrentHashMap时，不代表它的多个操作之间状态是一致的，是没有其他线程在同时操作。如果需要保证的话，要加锁。
     * 例如 size、isEmpty、containsValue等聚合方法，在并发情况下会反应conCurrentHashMap的中间状态。所以在并发情况下，这些方法只可以参考，而不可以控制流程
     * putAll这样的聚合方法，也不能确保原子性，在putAll的过程中，有可能获取到部分数据
     * stream.parallel()保证流的并发
     *
     * @return
     * @throws InterruptedException
     */
    @GetMapping("wrong")
    public String wrong() throws InterruptedException {
        //concurrentHashMap中先放入900个数
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        log.info("init size:{}", concurrentHashMap.size());

        //分别启用10个线程计算 还差多少个数到100.把数据补进去
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {
            int gap = ITEM_COUNT - concurrentHashMap.size();
            log.info("gap size:{}", gap);
            concurrentHashMap.putAll(getData(gap));
        }));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        log.info("finish size:{}", concurrentHashMap.size());
        return "OK";
    }

    /**
     * 针对上面的错错代码，对整段concurrentHashMap进行操作的代码段加锁
     * @return
     * @throws InterruptedException
     */
    @GetMapping("right")
    public String right() throws InterruptedException {
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        log.info("init size:{}", concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, 10).parallel().forEach(i -> {
            synchronized (concurrentHashMap) {
                int gap = ITEM_COUNT - concurrentHashMap.size();
                log.info("gap size:{}", gap);
                concurrentHashMap.putAll(getData(gap));
            }
        }));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        log.info("finish size:{}", concurrentHashMap.size());
        return "OK";
    }
}
