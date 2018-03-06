// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.rules.android;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration.Fragment;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.config.ConfigurationEnvironment;
import com.google.devtools.build.lib.analysis.config.ConfigurationFragmentFactory;
import com.google.devtools.build.lib.analysis.config.FragmentOptions;
import com.google.devtools.build.lib.analysis.config.InvalidConfigurationException;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.Strategy;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import javax.annotation.Nullable;

/** Configuration fragment for android_local_test. */
@AutoCodec
@Immutable
public class AndroidLocalTestConfiguration extends BuildConfiguration.Fragment {
  /** android_local_test specific options */
  @AutoCodec(strategy = Strategy.PUBLIC_FIELDS)
  public static final class Options extends FragmentOptions {
    @Option(
      name = "experimental_android_local_test_binary_resources",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
      effectTags = {OptionEffectTag.AFFECTS_OUTPUTS},
      help =
          "If true, provide Robolectric with binary resources instead of raw resources"
              + " for android_local_test. This should only be used by Robolectric team members"
              + " for testing purposes."
    )
    public boolean androidLocalTestBinaryResources;
  }

  /**
   * Loader class for {@link
   * com.google.devtools.build.lib.rules.android.AndroidLocalTestConfiguration}.
   */
  public static final class Loader implements ConfigurationFragmentFactory {

    @Nullable
    @Override
    public Fragment create(ConfigurationEnvironment env, BuildOptions buildOptions)
        throws InvalidConfigurationException, InterruptedException {
      return new AndroidLocalTestConfiguration(buildOptions.get(Options.class));
    }

    @Override
    public Class<? extends Fragment> creates() {
      return AndroidLocalTestConfiguration.class;
    }

    @Override
    public ImmutableSet<Class<? extends FragmentOptions>> requiredOptions() {
      return ImmutableSet.of(Options.class);
    }
  }

  private final boolean androidLocalTestBinaryResources;

  AndroidLocalTestConfiguration(Options options) {
    this.androidLocalTestBinaryResources = options.androidLocalTestBinaryResources;
  }

  @AutoCodec.Instantiator
  AndroidLocalTestConfiguration(boolean androidLocalTestBinaryResources) {
    this.androidLocalTestBinaryResources = androidLocalTestBinaryResources;
  }

  public boolean useAndroidLocalTestBinaryResources() {
    return this.androidLocalTestBinaryResources;
  }
}
