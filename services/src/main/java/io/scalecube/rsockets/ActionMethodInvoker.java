package io.scalecube.rsockets;

import io.scalecube.services.ServiceReference;
import io.scalecube.services.codecs.api.ServiceMessageCodec;

import io.rsocket.RSocket;

import org.reactivestreams.Publisher;

public abstract class ActionMethodInvoker<REQ, RESP> {

    protected final Class<REQ> reqType;
    protected final Class<RESP> respType;
    protected final CommunicationMode mode;

    protected RSocket client;
    protected ServiceMessageCodec payloadCodec;

    protected ActionMethodInvoker(Class<REQ> reqType, Class<RESP> respType, CommunicationMode mode, ServiceMessageCodec payloadCodec) {
        this.reqType = reqType;
        this.respType = respType;
        this.mode = mode;
        this.payloadCodec = payloadCodec;
    }

    abstract Publisher<RESP> invoke(ServiceReference serviceReference, REQ request);
}
