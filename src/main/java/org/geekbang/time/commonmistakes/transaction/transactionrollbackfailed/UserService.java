package org.geekbang.time.commonmistakes.transaction.transactionrollbackfailed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Slf4j
/**
 * 会有一些情况异常出现了也不会回滚
 * 1.只有异常传播出了标记了@Transactional注解的方法，事务才能回滚。
 * 2.默认情况下，出现RuntimeException(非受检异常)或Error的时候，Spring才会回滚事务
 *
 * 解决方法
 * 1. createUserRight1 手动让指定的异常都进行回滚（来把捕获的异常都进行回滚）
 * 2. createUserRight2 在注解中声明，期望遇到所有的Exception都回滚事务（来突破默认不回滚受检异常的限制）
 */
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void createUserWrong1(String name) {
        try {
            userRepository.save(new UserEntity(name));
            throw new RuntimeException("error");
        } catch (Exception ex) {
            log.error("create user failed", ex);
        }
    }

    @Transactional
    public void createUserWrong2(String name) throws IOException {
        userRepository.save(new UserEntity(name));
        otherTask();
    }

    private void otherTask() throws IOException {
        Files.readAllLines(Paths.get("file-that-not-exist"));
    }

    public int getUserCount(String name) {
        return userRepository.findByName(name).size();
    }


    @Transactional
    public void createUserRight1(String name) {
        try {
            userRepository.save(new UserEntity(name));
            throw new RuntimeException("error");
        } catch (Exception ex) {
            log.error("create user failed", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        log.info("result {} ", userRepository.findByName(name).size());//为什么这里是1你能想明白吗？
    }

    //DefaultTransactionAttribute
    @Transactional(rollbackFor = Exception.class)
    public void createUserRight2(String name) throws IOException {
        userRepository.save(new UserEntity(name));
        otherTask();
    }

}
