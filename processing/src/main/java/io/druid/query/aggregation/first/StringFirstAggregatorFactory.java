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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import io.druid.query.aggregation.AggregateCombiner;
import io.druid.query.aggregation.Aggregator;
import io.druid.query.aggregation.AggregatorFactory;
import io.druid.query.aggregation.AggregatorUtil;
import io.druid.query.aggregation.BufferAggregator;
import io.druid.query.aggregation.SerializablePairLongString;
import io.druid.query.cache.CacheKeyBuilder;
import io.druid.segment.ColumnSelectorFactory;
import io.druid.segment.column.Column;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonTypeName("stringFirst")
public class StringFirstAggregatorFactory extends AggregatorFactory
{
  public static final int DEFAULT_MAX_STRING_SIZE = 1024;

  public static final Comparator TIME_COMPARATOR = (o1, o2) -> Longs.compare(
      ((SerializablePairLongString) o1).lhs,
      ((SerializablePairLongString) o2).lhs
  );

  public static final Comparator VALUE_COMPARATOR = (o1, o2) -> {
    String s1 = null;
    String s2 = null;

    if (o1 != null) {
      s1 = ((SerializablePairLongString) o1).rhs;
    }
    if (o2 != null) {
      s2 = ((SerializablePairLongString) o2).rhs;
    }

    return Objects.equals(s1, s2) ? 1 : 0;
  };

  private final String fieldName;
  private final String name;
  protected final int maxStringBytes;

  @JsonCreator
  public StringFirstAggregatorFactory(
      @JsonProperty("name") String name,
      @JsonProperty("fieldName") final String fieldName,
      @JsonProperty("maxStringBytes") Integer maxStringBytes
  )
  {
    Preconditions.checkNotNull(name, "Must have a valid, non-null aggregator name");
    Preconditions.checkNotNull(fieldName, "Must have a valid, non-null fieldName");
    this.name = name;
    this.fieldName = fieldName;
    this.maxStringBytes = maxStringBytes == null ? DEFAULT_MAX_STRING_SIZE : maxStringBytes;
  }

  @Override
  public Aggregator factorize(ColumnSelectorFactory metricFactory)
  {
    return new StringFirstAggregator(
        metricFactory.makeColumnValueSelector(Column.TIME_COLUMN_NAME),
        metricFactory.makeColumnValueSelector(fieldName),
        maxStringBytes
    );
  }

  @Override
  public BufferAggregator factorizeBuffered(ColumnSelectorFactory metricFactory)
  {
    return new StringFirstBufferAggregator(
        metricFactory.makeColumnValueSelector(Column.TIME_COLUMN_NAME),
        metricFactory.makeColumnValueSelector(fieldName),
        maxStringBytes
    );
  }

  @Override
  public Comparator getComparator()
  {
    return VALUE_COMPARATOR;
  }

  @Override
  public Object combine(Object lhs, Object rhs)
  {
    return TIME_COMPARATOR.compare(lhs, rhs) > 0 ? lhs : rhs;
  }

  @Override
  public AggregateCombiner makeAggregateCombiner()
  {
    return new StringFirstAggregateCombiner();
  }

  @Override
  public AggregatorFactory getCombiningFactory()
  {
    return new StringFirstFoldingAggregatorFactory(name, name, maxStringBytes);
  }

  @Override
  public List<AggregatorFactory> getRequiredColumns()
  {
    return Arrays.asList(new StringFirstAggregatorFactory(fieldName, fieldName, maxStringBytes));
  }

  @Override
  public Object deserialize(Object object)
  {
    Map map = (Map) object;
    return new SerializablePairLongString(((Number) map.get("lhs")).longValue(), ((String) map.get("rhs")));
  }

  @Override
  public Object finalizeComputation(Object object)
  {
    return ((SerializablePairLongString) object).rhs;
  }

  @Override
  @JsonProperty
  public String getName()
  {
    return name;
  }

  @JsonProperty
  public String getFieldName()
  {
    return fieldName;
  }

  @JsonProperty
  public Integer getMaxStringBytes()
  {
    return maxStringBytes;
  }

  @Override
  public List<String> requiredFields()
  {
    return Arrays.asList(Column.TIME_COLUMN_NAME, fieldName);
  }

  @Override
  public byte[] getCacheKey()
  {
    return new CacheKeyBuilder(AggregatorUtil.STRING_FIRST_CACHE_TYPE_ID)
        .appendString(fieldName)
        .appendInt(maxStringBytes)
        .build();
  }

  @Override
  public String getTypeName()
  {
    return "serializablePairLongString";
  }

  @Override
  public int getMaxIntermediateSize()
  {
    return Long.BYTES + Integer.BYTES + maxStringBytes;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StringFirstAggregatorFactory that = (StringFirstAggregatorFactory) o;

    return fieldName.equals(that.fieldName) && name.equals(that.name) && maxStringBytes == that.maxStringBytes;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(name, fieldName, maxStringBytes);
  }

  @Override
  public String toString()
  {
    return "StringFirstAggregatorFactory{" +
           "name='" + name + '\'' +
           ", fieldName='" + fieldName + '\'' +
           ", maxStringBytes=" + maxStringBytes + '\'' +
           '}';
  }
}
