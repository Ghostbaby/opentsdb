// This file is part of OpenTSDB.
// Copyright (C) 2014-2017  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package net.opentsdb.data.types.numeric;

import com.google.common.reflect.TypeToken;

import net.opentsdb.data.MillisecondTimeStamp;
import net.opentsdb.data.TimeSeriesValue;
import net.opentsdb.data.TimeStamp;

/**
 * A simple mutable data point for holding primitive signed numbers including 
 * {@link Long}s or {@link Double}s.
 * 
 * @since 3.0
 */
public final class MutableNumericType implements NumericType, 
                                                 TimeSeriesValue<NumericType> {

  //NOTE: Fields are not final to make an instance available to store a new
  // pair of a timestamp and a value to reduce memory burden.
  
  /** The timestamp for this data point. */
  private TimeStamp timestamp;
  
  /** True if the value is stored as a long. */
  private boolean is_integer = true;
  
  /** A long value or a double encoded on a long if {@code is_integer} is false. */
  private long value = 0;
  
  /**
   * Initialize a new mutable data point with a {@link Long} value of 0.
   */
  public MutableNumericType() {
    timestamp = new MillisecondTimeStamp(0);
  }
  
  /**
   * Initialize the data point with a timestamp and value .
   * @param timestamp A non-null timestamp.
   * @param value A numeric value.
   * @throws IllegalArgumentException if the timestamp was null.
   */
  public MutableNumericType(final TimeStamp timestamp, 
                            final long value) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null.");
    }
    this.timestamp = timestamp.getCopy();
    this.value = value;
  }
  
  /**
   * Initialize the data point with a timestamp and value.
   * @param timestamp A non-null timestamp.
   * @param value A numeric value.
   * @throws IllegalArgumentException if the timestamp was null.
   */
  public MutableNumericType(final TimeStamp timestamp, 
                            final double value) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null.");
    }
    this.timestamp = timestamp.getCopy();
    this.value = Double.doubleToRawLongBits(value);
    is_integer = false;
  }

  /**
   * Initializes the data point with a timestamp and value, making copies of the
   * arguments.
   * @param timestamp A non-null timestamp.
   * @param value A numeric value.
   * @throws IllegalArgumentException if the timestamp or value was null.
   */
  public MutableNumericType(final TimeStamp timestamp, final NumericType value) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null.");
    }
    this.timestamp = timestamp.getCopy();
    if (value.isInteger()) {
      is_integer = true;
      this.value = value.longValue();
    } else {
      is_integer = false;
      this.value = Double.doubleToRawLongBits(value.doubleValue());
    }
  }
  
  /**
   * Initialize the data point from the given value, copying the timestamp
   * but passing the reference to the ID.
   * @param value A non-null value to copy from.
   * @throws IllegalArgumentException if the value was null.
   */
  public MutableNumericType(final TimeSeriesValue<NumericType> value) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null.");
    }
    timestamp = value.timestamp().getCopy();
    is_integer = value.value().isInteger();
    this.value = value.value().isInteger() ? value.value().longValue() : 
      Double.doubleToRawLongBits(value.value().doubleValue());
  }
  
  @Override
  public boolean isInteger() {
    return is_integer;
  }

  @Override
  public long longValue() {
    if (is_integer) {
      return value;
    }
    throw new ClassCastException("Not a long in " + toString());
  }

  @Override
  public double doubleValue() {
    if (!is_integer) {
      return Double.longBitsToDouble(value);
    }
    throw new ClassCastException("Not a double in " + toString());
  }

  @Override
  public double toDouble() {
    if (is_integer) {
      return value;
    }
    return Double.longBitsToDouble(value);
  }
  
  @Override
  public TimeStamp timestamp() {
    return timestamp;
  }

  @Override
  public NumericType value() {
    return this;
  }
  
  /**
   * Reset the value given the timestamp and value.
   * @param timestamp A non-null timestamp.
   * @param value A numeric value.
   * @throws IllegalArgumentException if the timestamp was null.
   */
  public void reset(final TimeStamp timestamp, final long value) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null");
    }
    this.timestamp.update(timestamp);
    this.value = value;
    is_integer = true;
  }
  
  /**
   * Reset the value given the timestamp and value.
   * @param timestamp A non-null timestamp.
   * @param value A numeric value.
   * @throws IllegalArgumentException if the timestamp was null.
   */
  public void reset(final TimeStamp timestamp, final double value) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null");
    }
    this.timestamp.update(timestamp);
    this.value = Double.doubleToRawLongBits(value);
    is_integer = false;
  }
  
  /**
   * Resets the local value by copying the timestamp and value from the source
   * value.
   * @param value A non-null value.
   * @throws IllegalArgumentException if the value was null or the value's 
   * timestamp was null.
   */
  public void reset(final TimeSeriesValue<NumericType> value) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }
    if (value.timestamp() == null) {
      throw new IllegalArgumentException("Value's timestamp cannot be null");
    }
    timestamp.update(value.timestamp());
    this.value = value.value().isInteger() ? value.value().longValue() : 
      Double.doubleToRawLongBits(value.value().doubleValue());
    is_integer = value.value().isInteger();
  }

  /**
   * Resets the local value by copying the timestamp and value from the arguments.
   * @param A non-null timestamp.
   * @param value A numeric value.
   * @throws IllegalArgumentException if the timestamp or value was null.
   */
  public void reset(final TimeStamp timestamp, final NumericType value) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null.");
    }
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null.");
    }
    this.timestamp.update(timestamp);
    if (value.isInteger()) {
      is_integer = true;
      this.value = value.longValue();
    } else {
      is_integer = false;
      this.value = Double.doubleToRawLongBits(value.doubleValue());
    }
  }
  
  @Override
  public TypeToken<NumericType> type() {
    return NumericType.TYPE;
  }

}