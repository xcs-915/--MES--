package com.tns.mes.common.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class StreamQueueService {
    private final RedisTemplate<String,Object> redis;
    public StreamQueueService(@Qualifier("mesRedisTemplate") RedisTemplate<String,Object> redis){this.redis=redis;}
    public RecordId publish(String stream, Map<String,String> body){try{return redis.opsForStream().add(MapRecord.create(stream, body));}catch(RuntimeException ex){return null;}}
    public Map<Object,Object> readLatest(String stream){try{MapRecord<String,Object,Object> record=redis.opsForStream().reverseRange(stream, org.springframework.data.domain.Range.unbounded()).stream().findFirst().orElse(null);return record==null?Collections.emptyMap():record.getValue();}catch(RuntimeException ex){return Collections.emptyMap();}}
}
