// Copyright 2014 The Bazel Authors. All rights reserved.
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
import com.google.common.collect.Interner;
import com.google.devtools.build.lib.actions.ActionLookupValue;
import com.google.devtools.build.lib.actions.Actions.GeneratingActions;
import com.google.devtools.build.lib.analysis.buildinfo.BuildInfoCollection;
import com.google.devtools.build.lib.analysis.buildinfo.BuildInfoFactory;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.SkyFunctionName;
import java.util.Objects;

/**
 * Value that stores {@link BuildInfoCollection}s generated by {@link BuildInfoFactory} instances.
 * These collections are used during analysis (see {@code CachingAnalysisEnvironment}).
 */
public class BuildInfoCollectionValue extends ActionLookupValue {
  private final BuildInfoCollection collection;

  BuildInfoCollectionValue(
      BuildInfoCollection collection,
      GeneratingActions generatingActions,
      boolean removeActionsAfterEvaluation) {
    super(generatingActions, removeActionsAfterEvaluation);
    this.collection = collection;
  }

  public BuildInfoCollection getCollection() {
    return collection;
  }

  @Override
  public String toString() {
    return getStringHelper().add("collection", collection).toString();
  }

  public static BuildInfoKeyAndConfig key(
      BuildInfoFactory.BuildInfoKey key, BuildConfiguration config) {
    return BuildInfoKeyAndConfig.create(key, ConfiguredTargetKey.keyFromConfiguration(config).key);
  }

  /** Key for BuildInfoCollectionValues. */
  @AutoCodec
  public static class BuildInfoKeyAndConfig extends ActionLookupKey {
    private static final Interner<BuildInfoKeyAndConfig> keyInterner =
        BlazeInterners.newWeakInterner();

    private final BuildInfoFactory.BuildInfoKey infoKey;
    private final BuildConfigurationValue.Key configKey;

    private BuildInfoKeyAndConfig(
        BuildInfoFactory.BuildInfoKey key, BuildConfigurationValue.Key configKey) {
      this.infoKey = Preconditions.checkNotNull(key, configKey);
      this.configKey = Preconditions.checkNotNull(configKey, key);
    }

    @AutoCodec.Instantiator
    static BuildInfoKeyAndConfig create(
        BuildInfoFactory.BuildInfoKey infoKey, BuildConfigurationValue.Key configKey) {
      return keyInterner.intern(new BuildInfoKeyAndConfig(infoKey, configKey));
    }

    @Override
    public SkyFunctionName functionName() {
      return SkyFunctions.BUILD_INFO_COLLECTION;
    }

    BuildInfoFactory.BuildInfoKey getInfoKey() {
      return infoKey;
    }

    BuildConfigurationValue.Key getConfigKey() {
      return configKey;
    }

    @Override
    public Label getLabel() {
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(infoKey, configKey);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (this.getClass() != other.getClass()) {
        return false;
      }
      BuildInfoKeyAndConfig that = (BuildInfoKeyAndConfig) other;
      return Objects.equals(this.infoKey, that.infoKey)
          && Objects.equals(this.configKey, that.configKey);
    }
  }
}
