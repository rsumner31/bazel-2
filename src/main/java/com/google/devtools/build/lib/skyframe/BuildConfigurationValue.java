// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.skyframe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Interner;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.config.FragmentClassSet;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;
import com.google.devtools.build.lib.skyframe.serialization.DeserializationContext;
import com.google.devtools.build.lib.skyframe.serialization.ObjectCodec;
import com.google.devtools.build.lib.skyframe.serialization.SerializationContext;
import com.google.devtools.build.lib.skyframe.serialization.SerializationException;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.VisibleForSerialization;
import com.google.devtools.build.lib.skyframe.serialization.strings.StringCodecs;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/** A Skyframe value representing a {@link BuildConfiguration}. */
// TODO(bazel-team): mark this immutable when BuildConfiguration is immutable.
// @Immutable
@AutoCodec
@ThreadSafe
public class BuildConfigurationValue implements SkyValue {
  private static final Interner<Key> keyInterner = BlazeInterners.newWeakInterner();

  private final BuildConfiguration configuration;

  BuildConfigurationValue(BuildConfiguration configuration) {
    this.configuration = configuration;
  }

  public BuildConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Returns the key for a requested configuration.
   *
   * @param fragments the fragments the configuration should contain
   * @param buildOptions the build options the fragments should be built from
   */
  @ThreadSafe
  public static Key key(
      Set<Class<? extends BuildConfiguration.Fragment>> fragments, BuildOptions buildOptions) {
    return key(
        FragmentClassSet.of(
            ImmutableSortedSet.copyOf(BuildConfiguration.lexicalFragmentSorter, fragments)),
        buildOptions);
  }

  public static Key key(FragmentClassSet fragmentClassSet, BuildOptions buildOptions) {
    return keyInterner.intern(new Key(fragmentClassSet, buildOptions));
  }

  /** {@link SkyKey} for {@link BuildConfigurationValue}. */
  @VisibleForSerialization
  public static final class Key implements SkyKey, Serializable {
    private final FragmentClassSet fragments;
    private final BuildOptions buildOptions;
    // If hashCode really is -1, we'll recompute it from scratch each time. Oh well.
    private volatile int hashCode = -1;

    private Key(FragmentClassSet fragments, BuildOptions buildOptions) {
      this.fragments = fragments;
      this.buildOptions = Preconditions.checkNotNull(buildOptions);
    }

    ImmutableSortedSet<Class<? extends BuildConfiguration.Fragment>> getFragments() {
      return fragments.fragmentClasses();
    }

    BuildOptions getBuildOptions() {
      return buildOptions;
    }

    @Override
    public SkyFunctionName functionName() {
      return SkyFunctions.BUILD_CONFIGURATION;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key otherConfig = (Key) o;
      return buildOptions.equals(otherConfig.buildOptions)
          && Objects.equals(fragments, otherConfig.fragments);
    }

    @Override
    public int hashCode() {
      if (hashCode == -1) {
        hashCode = Objects.hash(fragments, buildOptions);
      }
      return hashCode;
    }

    private static class Codec implements ObjectCodec<Key> {
      @Override
      public Class<Key> getEncodedClass() {
        return Key.class;
      }

      @Override
      public void serialize(SerializationContext context, Key obj, CodedOutputStream codedOut)
          throws SerializationException, IOException {
        context.serialize(obj.buildOptions, codedOut);
        codedOut.writeInt32NoTag(obj.fragments.fragmentClasses().size());
        for (Class<? extends BuildConfiguration.Fragment> fragment :
            obj.fragments.fragmentClasses()) {
          StringCodecs.asciiOptimized().serialize(context, fragment.getName(), codedOut);
        }
      }

      @Override
      @SuppressWarnings("unchecked") // Class<? extends...> cast
      public Key deserialize(DeserializationContext context, CodedInputStream codedIn)
          throws SerializationException, IOException {
        BuildOptions buildOptions = context.deserialize(codedIn);
        int fragmentsSize = codedIn.readInt32();
        ImmutableSortedSet.Builder<Class<? extends BuildConfiguration.Fragment>> fragmentsBuilder =
            ImmutableSortedSet.orderedBy(BuildConfiguration.lexicalFragmentSorter);
        for (int i = 0; i < fragmentsSize; i++) {
          try {
            fragmentsBuilder.add(
                (Class<? extends BuildConfiguration.Fragment>)
                    Class.forName(StringCodecs.asciiOptimized().deserialize(context, codedIn)));
          } catch (ClassNotFoundException e) {
            throw new SerializationException(
                "Couldn't deserialize BuildConfigurationValue$Key fragment class", e);
          }
        }
        return key(fragmentsBuilder.build(), buildOptions);
      }
    }
  }
}
