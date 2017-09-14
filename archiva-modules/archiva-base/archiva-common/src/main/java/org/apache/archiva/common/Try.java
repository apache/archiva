package org.apache.archiva.common;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.function.Function;

/**
 * This is a class that can be used for the Try monad
 *
 * The Try monad is able to collect exceptions during processing
 * of a stream.
 *
 *
 *
 */
public abstract class Try<V> {

    private Try() {

    }

    public abstract Boolean isSuccess();

    public abstract Boolean isFailure();

    public abstract void throwException();

    /**
     * Returns the value if this is a success instance. Otherwise throws
     * a runtime exception with the stored throwable as cause.
     *
     * @return The value
     */
    public abstract V get();

    /**
     * Returns the throwable that is stored in the failure.
     *
     * @return The Throwable or null.
     */
    public abstract Throwable getError();


    /**
     * A mapping method for mapping the current instance to a new type.
     *
     * @param fn
     * @param <U>
     * @return
     */
    public <U> Try<U> map(Function<? super V, U> fn) {
        try {
            return Try.success(fn.apply(get()));
        } catch (Throwable e) {
            return Try.failure(e);
        }
    }

    /**
     * This is the bind method.
     * If this instance is success the function will be applied. If any error occurs
     * a failure instance will be returned.
     * If this instance is failure a new failure will be returned.
     *
     * @param fn
     * @param <U>
     * @return
     */
    public <U> Try<U> flatMap(Function<? super V, Try<U>> fn) {
        try {
            return fn.apply(get());
        } catch (Throwable t) {
            return Try.failure(t);
        }
    }

    public static <V> Try<V> failure(String message) {

        return new Failure<>(message);

    }

    public static <V> Try<V> failure(String message, Throwable e) {

        return new Failure<>(message, e);

    }

    /**
     * If you need type coercion, you should call this method as
     *  Try.&lt;YOUR_TYPE&gt;failure(e)
     *
     *
     *
     * @param e The exception that is thrown
     * @param <V> The generic type this monad keeps
     * @return A new Try instance that represents a failure.
     */
    public static <V> Try<V> failure(Throwable e) {

        return new Failure<>(e);

    }


    /**
     * Returns a instance for the success case.
     *
     * @param value The value that should be stored.
     * @param <V> The return type
     * @return A new Try instance with the given value
     */
    public static <V> Try<V> success(V value) {

        return new Success<>(value);

    }

    private static class Failure<V> extends Try<V> {

        private Throwable exception;

        public Failure(String message) {

            super();

            this.exception = new IllegalStateException(message);

        }

        public Failure(String message, Throwable e) {

            super();

            this.exception = new IllegalStateException(message, e);

        }

        public Failure(Throwable e) {

            super();

            this.exception = new IllegalStateException(e);

        }

        @Override

        public Boolean isSuccess() {

            return false;

        }

        @Override

        public Boolean isFailure() {

            return true;

        }

        @Override

        public void throwException() {

            throw new RuntimeException(this.exception);

        }

        @Override
        public V get() {
            throw new RuntimeException(this.exception);
        }

        @Override
        public Throwable getError() {
            return exception;
        }
    }

    private static class Success<V> extends Try<V> {

        private V value;

        public Success(V value) {

            super();

            this.value = value;

        }

        @Override

        public Boolean isSuccess() {

            return true;

        }

        @Override

        public Boolean isFailure() {

            return false;

        }

        @Override

        public void throwException() {

            //log.error("Method throwException() called on a Success instance");

        }

        @Override
        public V get() {
            return value;
        }

        @Override
        public Throwable getError() {
            return null;
        }
    }

    // various method such as map an flatMap


    @Override
    public String toString() {
        return isSuccess() ? "true: "+get() : "false: "+ getError().getMessage();
    }
}
