/*
 * Copyright 2014-2016 Time Warner Cable, Inc.
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
package com.twcable.grabbit.tools.util;

import java.util.function.Supplier;

/**
 * An either/or (XOR) value represented as Left/Right.
 *
 * By convention, Left is an error value whereas Right is a success value.
 *
 * @param <L> the type of Left
 * @param <R> the type of Right
 */
@SuppressWarnings({"WeakerAccess", "unused", "PMD.AccessorClassGeneration"})
public interface Either<L, R> extends Supplier<R> {

    /**
     * Is this a {@link Left}?
     */
    boolean isLeft();

    /**
     * Get the value of this if it's a {@link Left}.
     *
     * @throws IllegalStateException if this is a {@link Right}
     */
    L getLeft();

    /**
     * Is this a {@link Right}?
     */
    boolean isRight();

    /**
     * Get the value of this if it's a {@link Right}.
     *
     * @throws IllegalStateException if this is a {@link Left}
     */
    R get();

    /**
     * Creates a {@link Left}.
     *
     * @param value the value to contain
     * @param <L>   the type of this {@link Left}
     * @param <R>   the {@link Right} type of this {@link Either}
     */
    static <L, R> Left<L, R> left(L value) {
        return new Left<>(value);
    }

    /**
     * Creates a {@link Right}.
     *
     * @param value the value to contain
     * @param <L>   the {@link Left} type of this {@link Either}
     * @param <R>   the type of this {@link Right}
     */
    static <L, R> Right<L, R> right(R value) {
        return new Right<>(value);
    }

    /**
     * By convention, this is typically the holder for an error.
     *
     * @param <L> the type this contains
     * @param <R> the {@link Right} type of this {@link Either}
     */
    class Left<L, R> implements Either<L, R> {
        private final L value;


        private Left(L value) {
            this.value = value;
        }


        /**
         * Returns true.
         */
        @Override
        public boolean isLeft() {
            return true;
        }


        /**
         * Get the value this contains.
         */
        @Override
        public L getLeft() {
            return value;
        }


        /**
         * Returns false.
         */
        @Override
        public boolean isRight() {
            return false;
        }


        /**
         * Throws {@link IllegalStateException} because this is not a {@link Right}.
         */
        @Override
        public R get() {
            throw new IllegalStateException("Called get() on a Either.Left");
        }
    }

    /**
     * By convention, this is typically the holder for a success value.
     *
     * @param <L> the {@link Left} type of this {@link Either}
     * @param <R> the type this contains
     */
    class Right<L, R> implements Either<L, R> {
        private final R value;


        private Right(R value) {
            this.value = value;
        }


        /**
         * Returns false.
         */
        @Override
        public boolean isLeft() {
            return false;
        }


        /**
         * Throws {@link IllegalStateException} because this is not a {@link Left}.
         */
        @Override
        public L getLeft() {
            throw new IllegalStateException("Called getLeft() on a Either.Right");
        }


        /**
         * Returns true.
         */
        @Override
        public boolean isRight() {
            return true;
        }


        /**
         * Get the value this contains.
         */
        @Override
        public R get() {
            return value;
        }
    }
}
