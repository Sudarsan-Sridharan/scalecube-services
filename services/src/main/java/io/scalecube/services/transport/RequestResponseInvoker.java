package io.scalecube.services.transport;

import io.scalecube.services.Reflect;
import io.scalecube.services.ServiceMessageCodec;
import io.scalecube.services.api.ServiceMessage;

import org.reactivestreams.Publisher;

import java.lang.reflect.Method;

import reactor.core.publisher.Mono;

public class RequestResponseInvoker extends AbstractServiceMethodInvoker<ServiceMessage, Publisher<ServiceMessage>> {


  public RequestResponseInvoker(Object serviceObject,
      Method method,
      Class<?> reqType, 
      Class<?> respType,
      ServiceMessageCodec<?> payloadCodec) {
    
    super(serviceObject, method, reqType, respType, payloadCodec);
  }

  public Publisher<ServiceMessage> invoke(ServiceMessage request) {
    
    ServiceMessage message = payloadCodec.decodeData(request, requestType);
    try {
      return Mono.from(Reflect.invokeMessage(serviceObject, method, message))
          .map(obj->toMessage(obj))
          .map(resp -> payloadCodec.encodeData(resp));
      
    } catch (Exception e) {
      return Mono.error(e);
    }
    
  }
}
