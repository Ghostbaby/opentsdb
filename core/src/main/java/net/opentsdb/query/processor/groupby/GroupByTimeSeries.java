// This file is part of OpenTSDB.
// Copyright (C) 2017  The OpenTSDB Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.opentsdb.query.processor.groupby;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;

import net.opentsdb.data.MergedTimeSeriesId;
import net.opentsdb.data.TimeSeries;
import net.opentsdb.data.TimeSeriesDataType;
import net.opentsdb.data.TimeSeriesId;
import net.opentsdb.data.TimeSeriesValue;
import net.opentsdb.query.processor.ProcessorFactory;

/**
 * A time series generated by the {@link GroupBy} processor. It must contain at
 * least one source before any of the iterator fetch functions are called. Once
 * {@link #id()}, {@link #types()}, {@link #iterator(TypeToken)} or 
 * {@link #iterators()} has been called, the implementation may not call
 * {@link #addSource(TimeSeries)}.
 * 
 * @since 3.0
 */
public class GroupByTimeSeries implements TimeSeries {
  /** The node this series belongs to. */
  protected final GroupBy node;
  
  /** The set of types in this series. */
  protected final Set<TypeToken<?>> types;
  
  /** The ID merger combining all of the time series IDs. */
  protected final MergedTimeSeriesId.Builder merging_id;
  
  /** The list of sources for this series. */
  protected Set<TimeSeries> sources;
  
  /** Whether or not the types have been unioned yet with the factories supported. */
  protected boolean types_unioned = false;
  
  /** The constructed time series ID. */
  protected TimeSeriesId id;
  
  /** Whether or not an iterator or iterators have been fetched. */
  protected boolean iterators_returned;
  
  /**
   * Default ctor.
   * @param node The Non-null node this series belongs to.
   */
  public GroupByTimeSeries(final GroupBy node) {
    if (node == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }
    this.node = node;
    types = Sets.newHashSetWithExpectedSize(1);
    merging_id = MergedTimeSeriesId.newBuilder();
  }
  
  /**
   * Package private method to add a source to this group by. DO NOT add a 
   * source after fetching an iterator, reading the types or fetching the ID.
   * @param source A non-null time series source.
   * @throws IllegalArgumentException if an iterator has been fetched, the 
   * types have been read, the ID read or the source was null.
   */
  void addSource(final TimeSeries source) {
    if (source == null) {
      throw new IllegalArgumentException("Source cannot be null.");
    }
    if (id != null || types_unioned || iterators_returned) {
      throw new IllegalArgumentException("Cannot add sources after the time "
          + "series has been used.");
    }
    if (sources == null) {
      sources = Sets.newHashSet();
    }
    merging_id.addSeries(source.id());
    sources.add(source);
    types.addAll(source.types());
  }
  
  @Override
  public TimeSeriesId id() {
    if (id == null) {
      id = merging_id.build();
    }
    return id;
  }

  @Override
  public Optional<Iterator<TimeSeriesValue<? extends TimeSeriesDataType>>> iterator(
      final TypeToken<?> type) {
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null.");
    }
    iterators_returned = true;
    if (!types.contains(type)) {
      return Optional.empty();
    }
    final Iterator<TimeSeriesValue<? extends TimeSeriesDataType>> iterator = 
        ((ProcessorFactory) node.factory()).newIterator(type, node, sources);
    if (iterator == null) {
      return Optional.empty();  
    }
    return Optional.of(iterator);
  }

  @Override
  public Collection<Iterator<TimeSeriesValue<? extends TimeSeriesDataType>>> iterators() {
    iterators_returned = true;
    final Collection<TypeToken<?>> types = types(); // calc the union
    final List<Iterator<TimeSeriesValue<? extends TimeSeriesDataType>>> iterators = 
        Lists.newArrayListWithCapacity(types.size());
    for (final TypeToken<?> type : types) {
      iterators.add(((ProcessorFactory) node.factory()).newIterator(type, node, sources));
    }
    return iterators;
  }

  @Override
  public Collection<TypeToken<?>> types() {
    if (!types_unioned) {
      final Collection<TypeToken<?>> factory_types = ((ProcessorFactory) node.factory()).types();
      final Iterator<TypeToken<?>> iterator = types.iterator();
      while (iterator.hasNext()) {
        if (!factory_types.contains(iterator.next())) {
          iterator.remove();
        }
      }
    }
    return types;
  }

  @Override
  public void close() {
    for (final TimeSeries ts : sources) {
      ts.close();
    }
  }
  
  @VisibleForTesting
  Set<TimeSeries> sources() {
    return sources;
  }
}