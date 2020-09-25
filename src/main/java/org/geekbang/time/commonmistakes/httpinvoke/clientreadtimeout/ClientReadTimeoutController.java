package org.geekbang.time.commonmistakes.httpinvoke.clientreadtimeout;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.fluent.Request;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("clientreadtimeout")
@Slf4j

/**
 * 连接超时参数ConnectTimeOut，让用户配置建立连接阶段的最长等待时间；关于连接超时参数和连接超时的误区，
 *      误区一：连接超时配置的特别长，比如60s.正常建立连接是毫秒级的响应，可以配置为1-5s。如果超时未连接成功则是有问题的
 *      误区二：排查连接问题时却不清楚连接的是哪里。首先要明白客户端是直接连接服务器，还是通过Nginx连接的服务端
 *
 *
 * 读取超时参数ReadTimeOut，用来控制从socket上读取数据的最长等待时间；关于读取超时参数和读取超时会有更多的误区
 *      误区一：认为出现了读取超时，服务端的执行就会中断（网络层面的超时和断开并不会影响服务器的执行）
 *      误区二：认为读取超时只是 Socket 网络层面的概念，是数据传输的最长耗时，故将其配置得非常短，比如 100 毫秒。
 *      （发生了读取超时，网络层面无法区分是服务端没有把数据返回给客户端，还是数据在网络上耗时比较久或者丢包）
 *      误区三：认为超时时间越长任务接口成功率就越高，将读取超时参数配置得太长。
 */
public class ClientReadTimeoutController {

    private String getResponse(String url, int connectTimeout, int readTimeout) throws IOException {
        return Request.Get("http://localhost:45678/clientreadtimeout" + url)
                .connectTimeout(connectTimeout)
                .socketTimeout(readTimeout)
                .execute()
                .returnContent()
                .asString();
    }

    @GetMapping("client")
    public String client() throws IOException {
        log.info("client1 called");
        //服务端5s超时，客户端读取超时2秒
        return getResponse("/server?timeout=5000", 1000, 2000);
    }

    @GetMapping("server")
    public void server(@RequestParam("timeout") int timeout) throws InterruptedException {
        log.info("server called");
        TimeUnit.MILLISECONDS.sleep(timeout);
        log.info("Done");
    }
}
