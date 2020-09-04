package org.geekbang.time.commonmistakes.lock.lockscope;

import lombok.Getter;

class Data {
    @Getter
    private static int counter = 0;
    private static Object locker = new Object();

    public static int reset() {
        counter = 0;
        return counter;
    }

    /**
     * synchronized 加给方法只能保证方法不会被多个线程同时执行
     * 在方法上加synchronized只能确保多个线程无法执行同一个实例的wrong方法，却不能保证不会执行不同实例的wrong方法。
     * 静态的counter在多个实例中共享，所以必然会出现线程安全问题。
     *
     * 静态字段要用静态方法（可以直接用类名.方法的那种）来保护
     */
    public synchronized void wrong() {
        counter++;
    }

    /**
     * 修改方法一、同样在类中定义一个静态的object,在操作counter之前对这个字段加锁。
     *
     * 修改方法二、将方法设置为静态的，因为会改变原来的代码结构，所以不采纳
     */
    public void right() {
        synchronized (locker) {
            counter++;
        }
    }
}