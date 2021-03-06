/*
 * Copyright 2017-2020 EPAM Systems, Inc. (https://www.epam.com/)
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

package com.epam.pipeline.entity.cluster.pool.filter.instancefilter;

import com.epam.pipeline.entity.cluster.pool.filter.value.StringCaseInsensitiveValueMatcher;
import com.epam.pipeline.entity.cluster.pool.filter.value.ValueMatcher;
import lombok.Data;

@Data
public class RunOwnerGroupPoolInstanceFilter implements StringInstanceFilter {

    private PoolInstanceFilterOperator operator;
    private String value;
    private PoolInstanceFilterType type = PoolInstanceFilterType.RUN_OWNER_GROUP;

    @Override
    public ValueMatcher<String> getMatcher() {
        return new StringCaseInsensitiveValueMatcher(value);
    }
}
