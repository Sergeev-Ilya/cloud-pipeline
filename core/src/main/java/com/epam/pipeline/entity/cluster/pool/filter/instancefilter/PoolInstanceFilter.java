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

import com.epam.pipeline.entity.cluster.pool.filter.value.ValueMatcher;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collection;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RunOwnerPoolInstanceFilter.class, name = "RUN_OWNER"),
        @JsonSubTypes.Type(value = RunOwnerGroupPoolInstanceFilter.class, name = "RUN_OWNER_GROUP"),
        @JsonSubTypes.Type(value = PipelinePoolInstanceFilter.class, name = "PIPELINE_ID"),
        @JsonSubTypes.Type(value = ConfigurationPoolInstanceFilter.class, name = "CONFIGURATION_ID"),
        @JsonSubTypes.Type(value = DockerPoolInstanceFilter.class, name = "DOCKER_IMAGE"),
        @JsonSubTypes.Type(value = ParameterPoolInstanceFilter.class, name = "RUN_PARAMETER")
    })
public interface PoolInstanceFilter<T> {

    PoolInstanceFilterOperator getOperator();

    T getValue();

    PoolInstanceFilterType getType();

    @JsonIgnore
    ValueMatcher<T> getMatcher();

    default boolean evaluate(T value) {
        return getOperator().evaluate(getMatcher(), value);
    }

    default boolean evaluate(Collection<T> value) {
        return getOperator().evaluate(getMatcher(), value);
    }
}
