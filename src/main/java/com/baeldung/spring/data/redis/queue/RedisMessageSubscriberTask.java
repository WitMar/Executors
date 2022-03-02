package com.baeldung.spring.data.redis.queue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RedisMessageSubscriberTask implements MessageListener {

    final static Logger logger = LoggerFactory.getLogger(RedisMessageSubscriberTask.class);
    final ConcurrentHashMap<Long, Long> state;
    final RedisMessagePublisherAck redisMessagePublisher;
    public RedisMessageSubscriberTask(ConcurrentHashMap<Long, Long> state,
                                      @Qualifier("publisherAck") RedisMessagePublisherAck redisMessagePublisher) {
        this.state = state;
        this.redisMessagePublisher = redisMessagePublisher;
    }

    @Override public synchronized void onMessage(Message message, byte[] bytes) {
        Long key = Long.valueOf(message.toString());
        synchronized (state){
            if (state.containsKey(key)) {
                Long value = state.get(key);
                if(value < 2) {
                    state.put(key, value + 1L);
                    notifyAll();
                }
                log.info("TaskMess: " + message);
            } else {
                state.put(key, 1L);
                notifyAll();
            }
        }
        logger.info("Task: " + message.toString() + " state " + (state.get(key)==1 ? "start" : "end"));
        redisMessagePublisher.publish(key);
    }
}