/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing;

/**
 * The general exception class throw when something goes wrong during decoding of a barcode.
 * This includes, but is not limited to, failing checksums / error correction algorithms, being
 * unable to locate finder timing patterns, and so on.
 *
 * @author Sean Owen
 */
public abstract class ReaderException extends Exception {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

ReaderException() {
    // do nothing
  }

  // Prevent stack traces from being taken
  // srowen says: huh, my IDE is saying this is not an override. native methods can't be overridden?
  // This, at least, does not hurt. Because we use a singleton pattern here, it doesn't matter anyhow.
  @Override
  public final Throwable fillInStackTrace() {
    return null;
  }

}
