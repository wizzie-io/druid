/*
 * Licensed to Metamarkets Group Inc. (Metamarkets) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Metamarkets licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.query.aggregation.first;

import io.druid.java.util.common.ISE;
import io.druid.query.aggregation.Aggregator;
import io.druid.query.aggregation.SerializablePairLongString;
import io.druid.segment.BaseLongColumnValueSelector;
import io.druid.segment.BaseObjectColumnValueSelector;

public class StringFirstAggregator implements Aggregator
{

  private final BaseObjectColumnValueSelector valueSelector;
  private final BaseLongColumnValueSelector timeSelector;
  private final int maxStringBytes;

  protected long firstTime;
  protected String firstValue;

  public StringFirstAggregator(
      BaseLongColumnValueSelector timeSelector,
      BaseObjectColumnValueSelector valueSelector,
      int maxStringBytes
  )
  {
    this.valueSelector = valueSelector;
    this.timeSelector = timeSelector;
    this.maxStringBytes = maxStringBytes;

    firstTime = Long.MAX_VALUE;
    firstValue = null;
  }

  @Override
  public void aggregate()
  {
    long time = timeSelector.getLong();
    if (time < firstTime) {
      firstTime = time;
      Object value = valueSelector.getObject();

      if (value instanceof String) {
        firstValue = (String) value;
      } else if (value instanceof SerializablePairLongString) {
        firstValue = ((SerializablePairLongString) value).rhs;
      } else if (value != null) {
        throw new ISE(
            "Try to aggregate unsuported class type [%s].Supported class types: String or SerializablePairLongString",
            value.getClass().getCanonicalName()
        );
      }

      if (firstValue != null && firstValue.length() > maxStringBytes) {
        firstValue = firstValue.substring(0, maxStringBytes);
      }
    }
  }

  @Override
  public Object get()
  {
    return new SerializablePairLongString(firstTime, firstValue);
  }

  @Override
  public float getFloat()
  {
    throw new UnsupportedOperationException("StringFirstAggregator does not support getFloat()");
  }

  @Override
  public long getLong()
  {
    throw new UnsupportedOperationException("StringFirstAggregator does not support getLong()");
  }

  @Override
  public double getDouble()
  {
    throw new UnsupportedOperationException("StringFirstAggregator does not support getDouble()");
  }

  @Override
  public void close()
  {
    // no resources to cleanup
  }
}
