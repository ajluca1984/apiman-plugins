/*
 * Copyright 2017 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apiman.plugins.auth3scale.authrep.strategies.impl;

import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.threescale.beans.BackendConfiguration;
import io.apiman.plugins.auth3scale.authrep.strategies.AbstractCachingAuthenticator;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * @author Marc Savy {@literal <msavy@redhat.com>}
 */
public class BatchedAuthCache extends AbstractCachingAuthenticator<AtomicInteger> {

    private static final AtomicInteger SENTINEL = new AtomicInteger(-1);
    private static final int DEFAULT_AUTHREP_COUNT = 5; // TODO make configurable

    public boolean shouldForceAsyncAuthRep(BackendConfiguration config, ApiRequest req, Object... elems) {
        return isAuthCached(config, req, elems);
    }

    @Override
    public boolean isAuthCached(BackendConfiguration config, ApiRequest req, Object... elems) {
        try {
            AtomicInteger val = lruCache.get(getCacheKey(config, req, elems), () -> SENTINEL);
            if (val == SENTINEL || val.get() <= 0) {
                return false;
            }
            return true;
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    @Override
    public BatchedAuthCache cache(BackendConfiguration config, ApiRequest req, Object... elems) {
        lruCache.put(getCacheKey(config, req, elems), new AtomicInteger(DEFAULT_AUTHREP_COUNT));
        return this;
    }

    @Override
    public BatchedAuthCache invalidate(BackendConfiguration config, ApiRequest req, Object... elems) {
        lruCache.invalidate(getCacheKey(config, req, elems));
        return this;
    }

    public int decrement(BackendConfiguration config, ApiRequest req, Object... elems) {
        try {
            int val = lruCache.get(getCacheKey(config, req, elems), () -> SENTINEL).getAndDecrement();
            if (val <= 0) {
                lruCache.invalidate(getCacheKey(config, req, elems));
            }
            return val;
        } catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

}
