/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.txn.proxy;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.parameters.DestroyProxyParameters;
import com.hazelcast.client.spi.impl.ClientInvocation;
import com.hazelcast.client.txn.TransactionContextProxy;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.partition.strategy.StringPartitioningStrategy;
import com.hazelcast.transaction.TransactionalObject;
import com.hazelcast.util.ExceptionUtil;

import java.util.concurrent.Future;

abstract class ClientTxnProxy implements TransactionalObject {

    final String objectName;
    final TransactionContextProxy proxy;

    ClientTxnProxy(String objectName, TransactionContextProxy proxy) {
        this.objectName = objectName;
        this.proxy = proxy;
    }

    final ClientMessage invoke(ClientMessage request) {
        try {
            ClientInvocation invocation = new ClientInvocation(proxy.getClient(), request, proxy.getConnection());
            Future<ClientMessage> future = invocation.invoke();
            return future.get();
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    protected String getTransactionId() {
        return proxy.getTxnId();
    }

    abstract void onDestroy();

    @Override
    public final void destroy() {
        onDestroy();
        ClientMessage request = DestroyProxyParameters.encode(objectName, getServiceName());
        invoke(request);
    }

    @Override
    public Object getId() {
        return objectName;
    }

    @Override
    public String getPartitionKey() {
        return StringPartitioningStrategy.getPartitionKey(getName());
    }

    Data toData(Object obj) {
        return proxy.getClient().getSerializationService().toData(obj);
    }

    Object toObject(Data data) {
        return proxy.getClient().getSerializationService().toObject(data);
    }
}
